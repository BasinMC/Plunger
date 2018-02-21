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
package org.basinmc.plunger.generator;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Optional;

/**
 * Provides documentation strings for classes.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface ClassJavaDocGenerator {

  /**
   * <p>Generates a JavaDoc documentation string for the specified class.</p>
   *
   * <p>The class name will be provided as a fully qualified name in its JVM format (e.g. each
   * element of the package path will be separated by a slash ("/") character. For instance,
   * "org.basinmc.plunger.Class" would be passed as "org/basinmc/plunger/Class". The return value is
   * expected to follow the same formatting.</p>
   *
   * @param className a reference to the enclosing class.
   * @return a field JavaDoc or, if no documentation is available, an empty optional.
   */
  @NonNull
  Optional<String> getClassDocumentation(@NonNull String className);
}
