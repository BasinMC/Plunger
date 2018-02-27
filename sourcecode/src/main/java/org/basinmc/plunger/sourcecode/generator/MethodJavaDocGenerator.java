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
package org.basinmc.plunger.sourcecode.generator;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Optional;

/**
 * Provides documentation strings for methods.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface MethodJavaDocGenerator {

  /**
   * <p>Generates a JavaDoc documentation string for the specified method.</p>
   *
   * <p>The class name and method signature will be passed in the JVM format (e.g. slashes ("/")
   * will be used instead of dots to separate package elements. For instance,
   * "org.basinmc.plunger.Class" will become "org/basinmc/plunger/Class").</p>
   *
   * <p>In case of method signatures, the return type and parameters will be specified within the
   * signature (for instance, a method of return type void and no parameters will be referenced as
   * "()V" while a method with the return type {@link Object} and a parameter of type boolean will
   * result in "(Z)Ljava/lang/Object").</p>
   *
   * @param className a reference to the enclosing class.
   * @param methodName a field name.
   * @param signature a field signature
   * @return a field JavaDoc or, if no documentation is available, an empty optional.
   */
  @NonNull
  Optional<String> getMethodDocumentation(@NonNull String className, @NonNull String methodName,
      @NonNull String signature);
}
