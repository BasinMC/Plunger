/*
 * Copyright 2018 Johannes Donath <johannesd@torchmind.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.basinmc.plunger.bytecode;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.basinmc.plunger.AbstractPlunger;
import org.basinmc.plunger.Plunger;
import org.basinmc.plunger.bytecode.transformer.BytecodeTransformer;
import org.basinmc.plunger.bytecode.transformer.BytecodeTransformer.Context;
import org.basinmc.plunger.bytecode.transformer.BytecodeTransformer.ClassMetadata;
import org.basinmc.plunger.bytecode.transformer.DelegatingClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a bytecode specific plunger implementation which is capable of transforming compiled
 * classes of JVM compatible languages.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class BytecodePlunger extends AbstractPlunger {

  private static final Logger logger = LoggerFactory.getLogger(BytecodePlunger.class);

  private final PathMatcher bytecodeMatcher;
  private final List<BytecodeTransformer> transformers;

  private BytecodePlunger(
      @NonNull Path source,
      @NonNull Path target,
      @NonNull Predicate<Path> classInclusionVoter,
      @NonNull Predicate<Path> transformationVoter,
      @NonNull Predicate<Path> resourceVoter,
      boolean sourceRelocation,
      boolean parallelism,
      @NonNull List<BytecodeTransformer> transformers) {
    super(source, target, classInclusionVoter, transformationVoter, resourceVoter,
        sourceRelocation, parallelism);
    this.transformers = new ArrayList<>(transformers);

    // since we do not know with which FileSystem we'll be dealing with, we'll have to initialize
    // the patch matcher here rather than relying on a static variable
    this.bytecodeMatcher = this.sourceFileSystem.getPathMatcher("glob:**.class");
  }

  /**
   * Creates a new empty bytecode plunger factory.
   *
   * @return a factory.
   */
  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void apply() throws IOException {
    ClassMetadata classMetadata = null;

    if (this.transformers.stream().anyMatch(BytecodeTransformer::usesClassMetadata)) {
      logger.info("Generating inheritance information ...");
      classMetadata = this.generateClassMetadata();
      logger.info("  SUCCESS");
    } else {
      logger.info("Inheritance information has not been collected");
    }

    logger.info("Applying transformations ...");
    Context context = new ContextImpl(classMetadata);

    Files.createDirectories(this.target);
    Stream<Path> stream =
        this.parallelism ? Files.walk(this.source).parallel() : Files.walk(this.source);

    Map<Path, IOException> failedExecutions = stream
        .flatMap((file) -> {
          try {
            // relativize the class path first so that we can resolve its target name easily
            Path source = this.source.relativize(file);
            Path target = this.target.resolve(source.toString());

            if (Files.isDirectory(file)) {
              return Stream.empty();
            }

            Files.createDirectories(target.getParent());

            if (this.bytecodeMatcher.matches(file)) {
              this.processClass(context, file, source, target);
            } else {
              this.processResource(file, source, target);
            }

            return Stream.empty();
          } catch (IOException ex) {
            return Stream.of(new SimpleImmutableEntry<>(file, ex));
          }
        })
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    // generate an exception which tells the user which exact files failed (with a hint to check the
    // log for more details)
    if (!failedExecutions.isEmpty()) {
      StringBuilder builder = new StringBuilder("One or more files failed to process:");
      builder.append(System.lineSeparator());

      failedExecutions.forEach((f, e) -> {
        builder.append(" * ");
        builder.append(f);
        builder.append(System.lineSeparator());
        builder.append("   Message: ").append(e.getMessage());
        builder.append(System.lineSeparator());
        builder.append("   Stacktrace: ");

        try (StringWriter writer = new StringWriter()) {
          try (PrintWriter printWriter = new PrintWriter(writer)) {
            e.printStackTrace(printWriter);
          }

          for (String line : NEWLINE_PATTERN.split(writer.toString())) {
            builder.append("     ").append(line).append(System.lineSeparator());
          }
        } catch (IOException ex) {
          builder.append("Unavailable: ").append(ex.getMessage());
        }
      });

      throw new IOException(builder.toString());
    }

    logger.info("Successfully applied transformations to all qualifying source files");
  }

  /**
   * Generates an inheritance map for the input classes within the source directory (including those
   * which would otherwise be excluded from the process).
   *
   * @return an inheritance map.
   * @throws IOException when reading from the input directory fails.
   */
  @NonNull
  private ClassMetadata generateClassMetadata() throws IOException {
    ClassMetadataImpl classMetadata = new ClassMetadataImpl();

    Iterator<Path> it = Files.walk(this.source)
        .filter(this.bytecodeMatcher::matches)
        .iterator();

    while (it.hasNext()) {
      try (InputStream inputStream = Files.newInputStream(it.next())) {
        ClassReader reader = new ClassReader(inputStream);
        reader.accept(new ClassVisitor(Opcodes.ASM6) {
          @Override
          public void visit(int version, int access, String name, String signature,
              String superName, String[] interfaces) {
            classMetadata.accessFlags.put(name, access);
            classMetadata.superTypes.put(name, superName);
            classMetadata.interfaces.computeIfAbsent(name, (k) -> new HashSet<>())
                .addAll(Arrays.asList(interfaces));
          }
        }, ClassReader.SKIP_DEBUG ^ ClassReader.SKIP_CODE ^ ClassReader.SKIP_FRAMES);
      }
    }

    return classMetadata;
  }

  /**
   * Retrieves a list of configured transformers within this Plunger instance.
   *
   * @return a list of transformers.
   */
  @NonNull
  public List<BytecodeTransformer> getTransformers() {
    return Collections.unmodifiableList(this.transformers);
  }

  /**
   * Copies or transforms a compiled class within the project based on the configured class and
   * transformation voters as well as the responses of the respective transformers.
   *
   * @param file a path to the currently processed resource.
   * @param source a relative path to the currently processed resource file.
   * @param target a relative path to the computed target file path.
   * @throws IOException when reading the source file or writing the target file fails.
   */
  private void processClass(@NonNull Context context, @NonNull Path file, @NonNull Path source,
      @NonNull Path target)
      throws IOException {
    logger.info("Processing class {} ...", file);

    try {
      // if the class voter chooses to not include this class at all, we'll abort early
      if (!this.isClassIncluded(file)) {
        logger.info("    SKIPPED");
        return;
      }

      // otherwise, we'll have to choose between a simple copy and a fully blown transformation
      if (!this.isTransformationDesired(file)) {
        logger.info("    COPIED");
        Files.copy(file, target);
        return;
      }

      // before we can transform the bytecode of our class, we'll have to create a pipeline consisting
      // of the visitors of all interested visitors
      //
      // to simplify things here a little bit (and to not expose too much implementation detail),
      // we'll simply use a delegation class visitor here
      DelegatingClassVisitor chainStart = new DelegatingClassVisitor();
      DelegatingClassVisitor previousVisitor = chainStart;
      DelegatingClassVisitor nextVisitor = null;

      for (BytecodeTransformer transformer : this.transformers) {
        nextVisitor = new DelegatingClassVisitor();
        ClassVisitor visitor = transformer.createTransformer(context, source, nextVisitor)
            .orElse(null);

        if (visitor != null) {
          previousVisitor.setVisitor(visitor);
          previousVisitor = nextVisitor;
        }
      }

      // if the first visitor in the chain has not been assigned a visitor, we'll simply copy the file
      // as well, as none of our transformers elected to change this class (e.g. passing it through
      // our chain wouldn't do anything apart from wasting time anyways)
      if (!chainStart.getVisitor().isPresent()) {
        logger.info("    COPIED");
        Files.copy(file, target);
        return;
      }

      // if we've been given a full chain of transformers, we'll construct our writer and begin the
      // transformation
      ClassWriter writer = new ClassWriter(0);
      ClassNameExtractorVisitor extractorVisitor = new ClassNameExtractorVisitor(writer);

      assert nextVisitor != null;
      nextVisitor.setVisitor(extractorVisitor);

      try (InputStream inputStream = Files.newInputStream(file)) {
        ClassReader reader = new ClassReader(inputStream);
        reader.accept(chainStart, ClassReader.EXPAND_FRAMES);
      }

      if (this.sourceRelocation && extractorVisitor.className != null) {
        target = this.target.resolve(extractorVisitor.className + ".class");
        logger.info("  Relocated to {}", target);
      }

      Files.createDirectories(target.getParent());

      try (WritableByteChannel outputChannel = Files
          .newByteChannel(target, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
              StandardOpenOption.TRUNCATE_EXISTING)) {
        outputChannel.write(ByteBuffer.wrap(writer.toByteArray()));
      }

      logger.info("    TRANSFORMED");
    } catch (IOException ex) {
      // re-throw any IO related exceptions here to make the log a little more pleasing to read even
      // when things go wrong
      logger.error("    FAILED");
      throw ex;
    }
  }


  /**
   * Provides a factory for configuring and constructing Plunger instances which are capable of
   * transforming arbitrary Bytecode of JVM compatible languages.
   */
  public static class Builder extends Plunger.Builder {

    private final List<BytecodeTransformer> transformers = new ArrayList<>();

    private Builder() {
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public BytecodePlunger build(@NonNull Path source, @NonNull Path target) {
      return new BytecodePlunger(
          source,
          target,
          this.classInclusionVoter,
          this.transformationVoter,
          this.resourceVoter,
          this.sourceRelocation,
          this.parallelism,
          this.transformers
      );
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withClassInclusionVoter(@NonNull Predicate<Path> voter) {
      super.withClassInclusionVoter(voter);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withParallelism() {
      super.withParallelism();
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withParallelism(boolean value) {
      super.withParallelism(value);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withResourceVoter(@NonNull Predicate<Path> voter) {
      super.withResourceVoter(voter);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withSourceRelocation(boolean value) {
      super.withSourceRelocation(value);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withTransformationVoter(@NonNull Predicate<Path> voter) {
      super.withTransformationVoter(voter);
      return this;
    }

    /**
     * Appends another Bytecode transformer to the configuration.
     *
     * @param transformer an arbitrary transformer implementation.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withTransformer(@NonNull BytecodeTransformer transformer) {
      this.transformers.add(transformer);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withoutResources() {
      super.withoutResources();
      return this;
    }
  }

  /**
   * Provides a visitor implementation which's sole purpose is to retrieve the new class name.
   */
  private static final class ClassNameExtractorVisitor extends ClassVisitor {

    private String className;

    private ClassNameExtractorVisitor(@NonNull ClassVisitor classVisitor) {
      super(Opcodes.ASM6, classVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);
      this.className = name;
    }
  }

  /**
   * Provides contextual information to transformers.
   */
  private static final class ContextImpl implements Context {

    private final ClassMetadata inheritanceMap;

    private ContextImpl(@Nullable ClassMetadata inheritanceMap) {
      this.inheritanceMap = inheritanceMap;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public ClassMetadata getClassMetadata() {
      if (this.inheritanceMap == null) {
        throw new IllegalStateException("Inheritance map was not generated");
      }

      return this.inheritanceMap;
    }
  }

  /**
   * Represents a cached version of the input's inheritance map.
   */
  private static final class ClassMetadataImpl implements ClassMetadata {

    private final Map<String, Integer> accessFlags = new HashMap<>();
    private final Map<String, String> superTypes = new HashMap<>();
    private final Map<String, Set<String>> interfaces = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAccess(@NonNull String className) {
      return this.accessFlags.getOrDefault(className, Opcodes.ACC_PUBLIC);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Set<String> getInterfaces(@NonNull String className) {
      return this.interfaces.getOrDefault(className, Collections.emptySet());
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getSuperClass(@NonNull String className) {
      return this.superTypes.getOrDefault(className, "java/lang/Object");
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Stream<String> walkInheritanceTree(@NonNull String className) {
      List<String> elements = new ArrayList<>();
      elements.add(className);

      for (String interfaceName : this.getInterfaces(className)) {
        elements.addAll(this.walkInterfaces(interfaceName));
      }

      do {
        className = this.getSuperClass(className);
        elements.add(className);
      } while (!"java/lang/Object".equals(className));

      return elements.stream();
    }

    @NonNull
    private List<String> walkInterfaces(@NonNull String className) {
      List<String> elements = new ArrayList<>();
      elements.add(className);

      for (String interfaceName : this.getInterfaces(className)) {
        elements.addAll(this.walkInterfaces(interfaceName));
      }

      return elements;
    }
  }
}
