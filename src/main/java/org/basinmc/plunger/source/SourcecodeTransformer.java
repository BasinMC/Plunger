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
package org.basinmc.plunger.source;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import org.jboss.forge.roaster.model.source.JavaSource;

/**
 * Performs an arbitrary transformation on a Java source file.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface SourcecodeTransformer {

  /**
   * Applies an arbitrary transformation to the passed class source code.
   *
   * @param source a relative path to the file in which this class is defined.
   * @param sourceType a parsed version of the class file.
   */
  void transform(@NonNull Path source, @NonNull JavaSource<?> sourceType);
}
