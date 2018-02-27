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
package org.basinmc.plunger.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nonnull;
import org.junit.After;
import org.junit.Before;

/**
 * Provides a base to test implementations which rely on a full Plunger project to operate.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class AbstractPlungerTest {

  private Path base;
  private Path source;
  private Path target;

  /**
   * Prepares a test directory with a source and target section in order to permit the evaluation of
   * a transformer.
   */
  @Before
  public void prepareTestFiles() throws IOException {
    this.base = Files.createTempDirectory("plunger_test_");
    this.source = this.base.resolve("source");
    this.target = this.base.resolve("target");

    Files.createDirectories(this.source);
    Files.createDirectories(this.target);
  }

  /**
   * Ensures that all files within the test directory will be correctly deleted at the end of the
   * test execution regardless of its outcome.
   */
  @After
  public void destroyTestFiles() throws IOException {
    Files.walk(this.base)
        .sorted((a, b) -> Math.min(1, Math.max(-1, b.getNameCount() - a.getNameCount())))
        .forEach((p) -> {
          try {
            Files.delete(p);
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        });
  }

  @Nonnull
  public Path getSource() {
    return this.source;
  }

  @Nonnull
  public Path getTarget() {
    return this.target;
  }

  /**
   * Extracts a test source file to the target directory.
   *
   * @param path a resource file path.
   * @param location a relative path to the target file.
   * @throws IOException when reading the test file or writing it to a temporary directory fails.
   */
  protected void extractSourceFile(@Nonnull String path, @Nonnull Path location)
      throws IOException {
    Path target = this.source.resolve(location);

    try (InputStream inputStream = this.getClass().getResourceAsStream(path)) {
      try (ReadableByteChannel inputChannel = Channels.newChannel(inputStream)) {
        try (FileChannel outputChannel = FileChannel
            .open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
          outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
        }
      }
    }
  }
}
