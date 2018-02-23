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
package org.basinmc.plunger;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.basinmc.plunger.bytecode.BytecodeTransformer;
import org.basinmc.plunger.source.SourcecodeTransformer;
import org.basinmc.plunger.source.formatter.SourcecodeFormatter;

/**
 * Applies a set of pre-configured transformers to an arbitrary archive or directory of either
 * compiled Java compatible bytecode or Java source files.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public interface Plunger {

  /**
   * Creates a new factory for bytecode based Plunger implementations.
   *
   * @return an empty builder.
   */
  @NonNull
  static BytecodeBuilder bytecodeBuilder() {
    return new BytecodeBuilder();
  }

  /**
   * Creates a class or resource exclusion voter which identifies whether or not a certain file is
   * to be included based on a set of patterns.
   *
   * @param fileSystem a file system.
   * @param patterns a set of patterns (prefixed with their respective algorithm).
   * @return a voter implementation.
   */
  @NonNull
  static Predicate<Path> createExclusionVoter(@NonNull FileSystem fileSystem,
      @NonNull Set<String> patterns) {
    return createExclusionVoter(
        patterns.stream()
            .map(fileSystem::getPathMatcher)
            .collect(Collectors.toSet())
    );
  }

  /**
   * Creates a class or resource exclusion voter which identifies whether or not a certain file is
   * to be included based on a path matcher.
   *
   * @param matchers a set of path matchers.
   * @return a voter implementation.
   */
  @NonNull
  static Predicate<Path> createExclusionVoter(@NonNull Set<PathMatcher> matchers) {
    final Set<PathMatcher> finalMatchers = new HashSet<>(matchers);
    return (p) -> finalMatchers.stream().noneMatch((m) -> m.matches(p));
  }

  /**
   * Creates a class or resource inclusion voter which identifies whether or not a certain file is
   * included based on a set of patterns.
   *
   * @param fileSystem a file system.
   * @param patterns a set of patterns (prefixed with their respective algorithm).
   * @return a voter implementation.
   */
  @NonNull
  static Predicate<Path> createInclusionVoter(@NonNull FileSystem fileSystem,
      @NonNull Set<String> patterns) {
    return createInclusionVoter(
        patterns.stream()
            .map(fileSystem::getPathMatcher)
            .collect(Collectors.toSet())
    );
  }

  /**
   * Creates a class or resource inclusion voter which identifies whether or not a certain file is
   * to be included based on a path matcher.
   *
   * @param matchers a set of path matchers.
   * @return a voter implementation.
   */
  @NonNull
  static Predicate<Path> createInclusionVoter(@NonNull Set<PathMatcher> matchers) {
    final Set<PathMatcher> finalMatchers = new HashSet<>(matchers);
    return (p) -> finalMatchers.stream().anyMatch((m) -> m.matches(p));
  }

  /**
   * Creates a new ZIP archive at the indicated location and exposes it as a NIO filesystem for use
   * within Plunger or other compatible implementations.
   *
   * @param filePath a path to the zip file.
   * @return a file system for the specified archive.
   * @throws URISyntaxException when the file URI is invalid.
   * @throws IOException when accessing the archive fails.
   */
  @NonNull
  static FileSystem createZipArchive(@NonNull Path filePath)
      throws URISyntaxException, IOException {
    Map<String, String> environment = new HashMap<>();
    environment.put("create", "true");

    return FileSystems.newFileSystem(new URI("jar:" + filePath.toUri()), environment);
  }

  /**
   * Opens an existing ZIP archive at the indicated location and exposes it as a NIO filesystem for
   * use within Plunger or other compatible implementations.
   *
   * @param filePath a path to the zip file.
   * @return a file system for the specified archive.
   * @throws URISyntaxException when the file URI is invalid.
   * @throws IOException when accessing the archive fails.
   */
  @NonNull
  static FileSystem openZipArchive(@NonNull Path filePath) throws URISyntaxException, IOException {
    return FileSystems.newFileSystem(new URI("jar:" + filePath.toUri()), Collections.emptyMap());
  }

  /**
   * Creates a new factory for bytecode based Plunger implementations.
   *
   * @return an empty builder.
   */
  @Nonnull
  static SourcecodeBuilder sourceBuilder() {
    return new SourcecodeBuilder();
  }

  /**
   * Applies the configured set of transformers to the codebase.
   *
   * @throws IOException when accessing one or more files or directories fails.
   */
  void apply() throws IOException;

  /**
   * Retrieves the source archive or directory in which the bytecode or Java source files, which are
   * consumed by the transformers, are located.
   */
  @NonNull
  Path getSource();

  /**
   * Retrieves the target archive or directory in which the bytecode or Java source files, which are
   * generated by this project, will be stored.
   */
  @NonNull
  Path getTarget();

  /**
   * Provides a factory for configuring and constructing Plunger instances.
   */
  abstract class Builder {

    protected Predicate<Path> classInclusionVoter = (p) -> true;
    protected Predicate<Path> resourceVoter = (p) -> true;
    protected Predicate<Path> transformationVoter = (p) -> true;
    protected boolean parallelism;
    protected boolean sourceRelocation = true;

    /**
     * Constructs a new Plunger with the specified source and target attributes.
     *
     * @param source a source path.
     * @param target a target path.
     * @return a plunger implementation.
     */
    @NonNull
    public abstract Plunger build(@NonNull Path source, @NonNull Path target);

    /**
     * Selects a class inclusion voter which evaluates whether or not to consider a class for
     * transformation and inclusion in the final result.
     *
     * @param voter a class inclusion voter.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withClassInclusionVoter(@NonNull Predicate<Path> voter) {
      this.classInclusionVoter = voter;
      return this;
    }

    /**
     * @see #withParallelism(boolean)
     */
    @Nonnull
    public Builder withParallelism() {
      return this.withParallelism(true);
    }

    /**
     * <p>Selects whether or not parallelism is desired (e.g. whether source files may be processed
     * on separate threads).</p>
     *
     * <p>Note that all transformers within the project are required to be thread safe in order for
     * parallel execution to work correctly (the visitors provided by bytecode transformers may,
     * however, be unsafe as they are ever invoked on more than one source file at a time).</p>
     *
     * @param value true if parallel execution is desired, false otherwise.
     * @return a reference to this builder.
     */
    @Nonnull
    public Builder withParallelism(boolean value) {
      this.parallelism = value;
      return this;
    }

    /**
     * Selects a resource voter which evaluates whether or not to copy a certain resource to the
     * target directory while iterating the existing class files.
     *
     * @param voter a resource voter.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withResourceVoter(@NonNull Predicate<Path> voter) {
      this.resourceVoter = voter;
      return this;
    }

    /**
     * Selects a transformation voter which evaluates whether or not to transform a certain class
     * file.
     *
     * @param voter a class transformation voter.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withTransformationVoter(@NonNull Predicate<Path> voter) {
      this.transformationVoter = voter;
      return this;
    }

    /**
     * Selects a resource voter which prevents all resources from being copied to the target
     * directory.
     *
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withoutResources() {
      this.resourceVoter = (p) -> false;
      return this;
    }

    /**
     * Selects whether or not to relocate the source files within the target path based on their new
     * names.
     *
     * @param value true if relocation is desired, false otherwise.
     * @return a reference to this builder.
     */
    @Nonnull
    public Builder withSourceRelocation(boolean value) {
      this.sourceRelocation = value;
      return this;
    }
  }

  /**
   * Provides a factory for configuring and constructing Plunger instances which are capable of
   * transforming arbitrary Bytecode of JVM compatible languages.
   */
  class BytecodeBuilder extends Builder {

    private final List<BytecodeTransformer> transformers = new ArrayList<>();

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
    public BytecodeBuilder withClassInclusionVoter(@NonNull Predicate<Path> voter) {
      super.withClassInclusionVoter(voter);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public BytecodeBuilder withParallelism() {
      super.withParallelism();
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public BytecodeBuilder withParallelism(boolean value) {
      super.withParallelism(value);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public BytecodeBuilder withResourceVoter(@NonNull Predicate<Path> voter) {
      super.withResourceVoter(voter);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public BytecodeBuilder withTransformationVoter(@NonNull Predicate<Path> voter) {
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
    public BytecodeBuilder withTransformer(@NonNull BytecodeTransformer transformer) {
      this.transformers.add(transformer);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public BytecodeBuilder withoutResources() {
      super.withoutResources();
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public BytecodeBuilder withSourceRelocation(boolean value) {
      super.withSourceRelocation(value);
      return this;
    }
  }

  /**
   * Provides a factory for configuring and constructing Plunger instances which are capable of
   * transforming arbitrary Java source code.
   */
  class SourcecodeBuilder extends Builder {

    private final List<SourcecodeTransformer> transformers = new ArrayList<>();
    private SourcecodeFormatter formatter = SourcecodeFormatter.noop();
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public SourcecodePlunger build(@NonNull Path source, @NonNull Path target) {
      return new SourcecodePlunger(
          source,
          target,
          this.classInclusionVoter,
          this.transformationVoter,
          this.resourceVoter,
          this.sourceRelocation,
          this.parallelism,
          this.formatter,
          this.charset,
          this.transformers
      );
    }

    /**
     * Selects a specific charset to use when en- and decoding source files.
     *
     * @param charset a charset.
     * @return a reference to this builder.
     */
    @Nonnull
    public SourcecodeBuilder withCharset(@Nonnull Charset charset) {
      this.charset = charset;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public SourcecodeBuilder withClassInclusionVoter(@NonNull Predicate<Path> voter) {
      super.withClassInclusionVoter(voter);
      return this;
    }

    /**
     * Selects a special source code formatter which is applied when re-encoding the class files
     * (note that only classes which are selected for transformation will be formatted).
     *
     * @param formatter a custom formatter implementation.
     * @return a reference to this builder.
     */
    @Nonnull
    public SourcecodeBuilder withFormatter(@Nonnull SourcecodeFormatter formatter) {
      this.formatter = formatter;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public SourcecodeBuilder withParallelism() {
      super.withParallelism();
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public SourcecodeBuilder withParallelism(boolean value) {
      super.withParallelism(value);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public SourcecodeBuilder withResourceVoter(@NonNull Predicate<Path> voter) {
      super.withResourceVoter(voter);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public SourcecodeBuilder withTransformationVoter(@NonNull Predicate<Path> voter) {
      super.withTransformationVoter(voter);
      return this;
    }

    /**
     * Appends an arbitrary source code transformer to the new Plunger instance.
     *
     * @param transformer a transformer.
     * @return a reference to this builder.
     */
    @Nonnull
    public SourcecodeBuilder withTransformer(@Nonnull SourcecodeTransformer transformer) {
      this.transformers.add(transformer);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public SourcecodeBuilder withoutResources() {
      super.withoutResources();
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public SourcecodeBuilder withSourceRelocation(boolean value) {
      super.withSourceRelocation(value);
      return this;
    }
  }
}
