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
package org.basinmc.plunger.bytecode;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.Optional;
import org.objectweb.asm.ClassVisitor;

/**
 * Performs an arbitrary transformation to a set of compiled Java code (or of a JVM compatible
 * language).
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
@FunctionalInterface
public interface BytecodeTransformer {

  /**
   * <p>Creates a new transformer for the specified class file.</p>
   *
   * <p>The class name will be provided as a fully qualified name in its JVM format (e.g. each
   * element of the package path will be separated by a slash ("/") character. For instance,
   * "org.basinmc.plunger.Class" would be passed as "org/basinmc/plunger/Class".</p>
   *
   * @param source a relative path to the class file.
   * @return a visitor which is to be applied to the class bytecode.
   */
  Optional<ClassVisitor> createTransformer(@NonNull Path source, @NonNull ClassVisitor nextVisitor);
}
