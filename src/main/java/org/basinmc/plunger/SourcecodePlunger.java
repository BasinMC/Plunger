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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.basinmc.plunger.source.SourcecodeTransformer;
import org.basinmc.plunger.source.formatter.SourcecodeFormatter;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class SourcecodePlunger extends AbstractPlunger {

  private static final Logger logger = LoggerFactory.getLogger(SourcecodePlunger.class);

  private final PathMatcher classMatcher;
  private final SourcecodeFormatter formatter;
  private final Charset charset;
  private final List<SourcecodeTransformer> transformers;

  SourcecodePlunger(@NonNull Path source,
      @NonNull Path target,
      @NonNull Predicate<Path> classInclusionVoter,
      @NonNull Predicate<Path> transformationVoter,
      @NonNull Predicate<Path> resourceVoter,
      boolean sourceRelocation,
      boolean parallelism,
      @Nonnull SourcecodeFormatter formatter,
      @Nonnull Charset charset,
      @Nonnull List<SourcecodeTransformer> transformers) {
    super(source, target, classInclusionVoter, transformationVoter, resourceVoter,
        sourceRelocation, parallelism);
    this.formatter = formatter;
    this.charset = charset;
    this.transformers = new ArrayList<>(transformers);

    this.classMatcher = this.sourceFileSystem.getPathMatcher("glob:**.java");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void apply() throws IOException {
    logger.info("Applying transformations ...");

    Files.createDirectories(this.target);

    Stream<Path> stream;
    if (this.parallelism) {
      stream = Files.walk(this.source).parallel();
    } else {
      stream = Files.walk(this.source);
    }

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

            if (this.classMatcher.matches(file)) {
              this.processSourceFile(file, source, target);
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

    Iterator<Path> it;

    if (this.parallelism) {
      it = Files.walk(this.source).parallel().iterator();
    } else {
      it = Files.walk(this.source).iterator();
    }

    while (it.hasNext()) {
      Path file = it.next();
    }

    logger.info("Successfully applied transformations to all qualifying source files");
  }

  /**
   * Copies or transforms a class within the project based on the configured class and
   * transformation voters as well as the responses of the respective transformers.
   *
   * @param file a path to the currently processed resource.
   * @param source a relative path to the currently processed resource file.
   * @param target a relative path to the computed target file path.
   * @throws IOException when reading the source file or writing the target file fails.
   */
  private void processSourceFile(@Nonnull Path file, @Nonnull Path source, @Nonnull Path target)
      throws IOException {
    logger.info("Processing class {} ...", file);

    try {
      // ensure the class has been selected for inclusion at all and if not skip the execution
      // entirely to save some time
      if (!this.isClassIncluded(file)) {
        logger.info("    SKIPPED");
        return;
      }

      // when no transformation is desired, we'll just copy the file to the target directory as is
      // to save some time
      if (!this.isTransformationDesired(file)) {
        Files.copy(file, target);
        logger.info("    COPIED");
        return;
      }

      // since we actually desire transformation (and are incapable of identifying whether or not a
      // transformer is actually interested in a class), we'll have to parse the file and apply all
      // transformers
      JavaSource<?> type;

      String code = new String(Files.readAllBytes(file), this.charset);
      type = Roaster.parse(JavaSource.class, code);

      for (SourcecodeTransformer transformer : this.transformers) {
        transformer.transform(source, type);
      }

      // reformat the code and pass it to the selected java code formatter (we generally encode
      // everything as UTF-8 since it's typically used anyways
      if (this.sourceRelocation) {
        target = this.target.resolve(type.getQualifiedName().replace('.', '/') + ".java");
        logger.info("  Relocated to {}", target);
      }

      Files.createDirectories(target.getParent());
      Files.write(target, this.formatter.format(type.toString()).getBytes(this.charset));
      logger.info("    TRANSFORMED");
    } catch (IOException ex) {
      logger.error("    FAILED");
      throw ex;
    }
  }
}
