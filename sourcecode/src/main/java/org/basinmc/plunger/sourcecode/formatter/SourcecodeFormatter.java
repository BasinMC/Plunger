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
package org.basinmc.plunger.sourcecode.formatter;

import javax.annotation.Nonnull;

/**
 * Provides means to format arbitrary source code into a pre-defined format.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface SourcecodeFormatter {

  /**
   * Reformats the passed source code into a pre-defined format (typically backed by a Styleguide
   * document).
   *
   * @param source the full class source code.
   * @return a formatted version of the source code.
   */
  @Nonnull
  String format(@Nonnull String source);

  /**
   * Creates a new no-operation code formatter which passes the code back as-is (e.g. with native
   * formatting).
   *
   * @return a no-op source code formatter.
   */
  @Nonnull
  static SourcecodeFormatter noop() {
    return (source) -> source;
  }
}
