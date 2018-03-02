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
package org.basinmc.plunger.mapping;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Optional;

/**
 * Resolves one or more class access mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface ClassAccessMapping {

  /**
   * <p>Retrieves the target access flags for the specified class.</p>
   *
   * <p>The class name will be provided as a fully qualified name in its JVM format (e.g. each
   * element of the package path will be separated by a slash ("/") character. For instance,
   * "org.basinmc.plunger.Class" would be passed as "org/basinmc/plunger/Class". The return value is
   * expected to follow the same formatting.</p>
   *
   * <p>When no mapping is defined for the indicated class, an empty optional should be returned
   * instead.</p>
   *
   * @param className a class name.
   * @param flags the current access flags.
   * @return the target access flags or, if no change is desired, an empty optional.
   */
  @NonNull
  Optional<AccessFlag> getClassAccessFlags(@NonNull String className, @NonNull AccessFlag flags);

  /**
   * <p>Retrieves an inverse version of the mapping.</p>
   *
   * <p>The resulting mapping will effectively reverse the effects of this mapping (e.g. a mapping
   * which marks the private class "org.basinmc.plunger.Class" as public will mark the same class
   * private again in its inversion).</p>
   *
   * @return an inverse mapping.
   * @throws UnsupportedOperationException when the operation is not supported by this
   * implementation.
   */
  @NonNull
  default ClassAccessMapping invert() {
    throw new UnsupportedOperationException();
  }
}
