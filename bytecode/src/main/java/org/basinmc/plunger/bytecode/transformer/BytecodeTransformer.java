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
package org.basinmc.plunger.bytecode.transformer;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
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
   * @param context a set of contextual information.
   * @param nextVisitor the upcoming visitor (this visitor MUST be called by the returned class
   * visitor).
   * @return a visitor which is to be applied to the class bytecode.
   */
  @SuppressWarnings("deprecation")
  Optional<ClassVisitor> createTransformer(@NonNull Context context, @NonNull Path source,
      @NonNull ClassVisitor nextVisitor);

  /**
   * <p>Evaluates whether this transformer relies on class metadata (such as inheritance
   * trees).</p>
   *
   * <p>When no transformer returns true within this method, the generation of class metadata will
   * be skipped.</p>
   *
   * @return true if class metadata is used, false otherwise.
   */
  default boolean usesClassMetadata() {
    return false;
  }

  /**
   * Provides additional contextual information to a transformer.
   */
  interface Context {

    /**
     * Retrieves a map of class metadata such as inheritance and current access levels.
     *
     * @return an inheritance map.
     * @throws IllegalStateException when no transformer declared that it wishes to use inheritance
     * information.
     */
    @NonNull
    ClassMetadata getClassMetadata();
  }

  /**
   * Provides a map of class metadata such as inheritance or access levels.
   */
  interface ClassMetadata {

    /**
     * Retrieves the original access level for the specified class.
     *
     * @param className a class name.
     * @return an access level (as defined by the {@link org.objectweb.asm.Opcodes} {@code ACC_}
     * constants).
     */
    int getAccess(@NonNull String className);

    /**
     * Retrieves the original access level for the specified field.
     *
     * @param owner a field owner (e.g. the enclosing class).
     * @param name a field name.
     * @param desc a field descriptor.
     * @return an access level (as defined by the {@link org.objectweb.asm.Opcodes} {@code ACC_}
     * constants).
     */
    Optional<Integer> getFieldAccess(@NonNull String owner, @NonNull String name,
        @NonNull String desc);

    /**
     * Retrieves the original access level for the specified method.
     *
     * @param owner a field owner (e.g. the enclosing class).
     * @param name a field name.
     * @param desc a field descriptor.
     * @return an access level (as defined by the {@link org.objectweb.asm.Opcodes} {@code ACC_}
     * constants).
     */
    Optional<Integer> getMethodAccess(@NonNull String owner, @NonNull String name,
        @NonNull String desc);

    /**
     * Retrieves the set of interfaces the class with the specified name is directly inheriting
     * from.
     *
     * @param className a class name.
     * @return a set of interfaces.
     */
    @NonNull
    Set<String> getInterfaces(@NonNull String className);

    /**
     * <p>Retrieves the direct super type for the specified class name.</p>
     *
     * <p>When the class does not explicitly define a class that it inherits from, {@code
     * java/lang/Object} will be returned instead.</p>
     *
     * @param className a class name.
     * @return a super class.
     */
    @NonNull
    String getSuperClass(@NonNull String className);

    /**
     * <p>Walks the complete inheritance path of the specified class starting with the class
     * itself.</p>
     *
     * <p>The inherited classes will be walked in the following order:</p>
     *
     * <ol>
     *
     * <li>Self (e.g. the passed class name)</li>
     *
     * <li>Interfaces (and their super interfaces)</li>
     *
     * <li>Super-Classes (and their respective super interfaces)</li>
     *
     * </ol>
     *
     * <p>Note that duplicates will be included in the stream unfiltered.</p>
     *
     * @param className a class name.
     * @return a stream of class names from which the specified class inherits members.
     */
    @NonNull
    Stream<String> walkInheritanceTree(@NonNull String className);
  }
}
