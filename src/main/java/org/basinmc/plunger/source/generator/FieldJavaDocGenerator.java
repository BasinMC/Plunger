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
package org.basinmc.plunger.source.generator;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Optional;

/**
 * Provides documentation strings for fields.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface FieldJavaDocGenerator {

  /**
   * <p>Generates a JavaDoc documentation string for the specified field.</p>
   *
   * <p>The field class name and signature will be passed in the JVM format (e.g. class name
   * elements will be separated by a slash ("/") instead of dots. For instance,
   * "org.basinmc.plunger.Class" would become "org/basinmc/plunger/Class").</p>
   *
   * <p>The field signature consist of the full Bytecode type signature of the field (for instance,
   * a field of type boolean will be specified as "Z" or a field of type {@link Object} will be
   * specified as "Ljava/lang/Object").</p>
   *
   * @param className a reference to the enclosing class.
   * @param fieldName a field name.
   * @param signature a field signature
   * @return a field JavaDoc or, if no documentation is available, an empty optional.
   */
  @NonNull
  Optional<String> getFieldDocumentation(@NonNull String className, @NonNull String fieldName,
      @NonNull String signature);
}
