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
package org.basinmc.plunger.common.mapping;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Optional;

/**
 * Resolves one or more field name mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface FieldNameMapping {

  /**
   * <p>Retrieves the target name for a specified field.</p>
   *
   * <p>The field class name and signature will be passed in the JVM format (e.g. class name
   * elements will be separated by a slash ("/") instead of dots. For instance,
   * "org.basinmc.plunger.Class" would become "org/basinmc/plunger/Class").</p>
   *
   * <p>The field signature will consist of the full Bytecode type signature of the field (for
   * instance, a field of type boolean will be specified as "Z" or a field of type {@link Object}
   * will be specified as "Ljava/lang/Object").</p>
   *
   * @param className the containing class name.
   * @param fieldName the original field name.
   * @param signature the field signature.
   * @return a mapped field name or, if no remapping is desired, an empty optional.
   */
  @NonNull
  Optional<String> getFieldName(@NonNull String className, @NonNull String fieldName,
      @NonNull String signature);

  /**
   * <p>Retrieves an inverse mapping of this mapping.</p>
   *
   * <p>The resulting mapping will effectively reverse the effects of this mapping (for instance, a
   * field mapping which renames the field from "localField" to "a" would result in an inverse
   * mapping of "a" to "localField").</p>
   *
   * @return an inverse mapping.
   * @throws UnsupportedOperationException when the operation is not supported by this
   * implementation.
   */
  @NonNull
  default FieldNameMapping invert() {
    throw new UnsupportedOperationException();
  }
}
