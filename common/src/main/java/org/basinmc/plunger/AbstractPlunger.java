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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides commonly required methods and utilities to Plunger implementations.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class AbstractPlunger implements Plunger {

  protected static final Pattern NEWLINE_PATTERN = Pattern.compile("\r?\n");
  protected final Predicate<Path> classInclusionVoter;
  protected final boolean parallelism;
  protected final Predicate<Path> resourceVoter;
  protected final Path source;
  protected final FileSystem sourceFileSystem;
  protected final boolean sourceRelocation;
  protected final Path target;
  protected final FileSystem targetFileSystem;
  protected final Predicate<Path> transformationVoter;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected AbstractPlunger(
      @NonNull Path source,
      @NonNull Path target,
      @NonNull Predicate<Path> classInclusionVoter,
      @NonNull Predicate<Path> transformationVoter,
      @NonNull Predicate<Path> resourceVoter,
      boolean sourceRelocation,
      boolean parallelism) {
    this.source = source;
    this.target = target;

    this.classInclusionVoter = classInclusionVoter;
    this.transformationVoter = transformationVoter;
    this.resourceVoter = resourceVoter;
    this.sourceRelocation = sourceRelocation;
    this.parallelism = parallelism;

    this.sourceFileSystem = source.getFileSystem();
    this.targetFileSystem = target.getFileSystem();
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Path getSource() {
    return this.source;
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Path getTarget() {
    return this.target;
  }

  /**
   * Evaluates whether the configured class inclusion voter chooses to include the class file at the
   * specified location.
   *
   * @param path a class file path (relative to source path or absolute).
   * @return true if included, false otherwise.
   */
  protected boolean isClassIncluded(@NonNull Path path) {
    if (path.isAbsolute()) {
      path = this.source.relativize(path);
    }

    return this.classInclusionVoter.test(path);
  }

  /**
   * Evaluates whether the configured resource inclusion voter chooses to include the resource file
   * at the specified location.
   *
   * @param path a resource file path (relative to source path or absolute).
   * @return true if included, false otherwise.
   */
  protected boolean isResourceIncluded(@NonNull Path path) {
    if (path.isAbsolute()) {
      path = this.source.relativize(path);
    }

    return this.resourceVoter.test(path);
  }

  /**
   * Evaluates whether the configured class transformation voter chooses to transform the class file
   * at the specified location.
   *
   * @param path a class file path (relative to source path or absolute).
   * @return true if transformation is desired, false otherwise.
   */
  protected boolean isTransformationDesired(@NonNull Path path) {
    if (path.isAbsolute()) {
      path = this.source.relativize(path);
    }

    return this.transformationVoter.test(path);
  }

  /**
   * Copies a resource file within the project to the output directory.
   *
   * @param file a path to the currently processed resource.
   * @param source a relative path to the currently processed resource file.
   * @param target a relative path to the computed target file path.
   * @throws IOException when reading or writing the source file or writing the target file fails.
   */
  protected void processResource(@NonNull Path file, @Nonnull Path source, @Nonnull Path target)
      throws IOException {
    this.logger.info("Processing resource {} ...", file);

    try {
      // if the resource voter chooses not to include the resource in the result, we'll skip the
      // execution entirely to save some time
      if (!this.isResourceIncluded(file)) {
        this.logger.info("    SKIPPED");
        return;
      }

      // otherwise we can simply copy the file as-is
      // TODO: Add support for resource file transformation
      Files.copy(file, target);

      this.logger.info("    COPIED");
    } catch (IOException ex) {
      this.logger.info("    FAILED");
      throw ex;
    }
  }
}
