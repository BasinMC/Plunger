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
import javax.annotation.Nonnull;
import org.basinmc.plunger.common.mapping.NameMapping;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Provides a bytecode transformer which remaps the names of classes based on a pre-configured set
 * of mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class MappingBytecodeTransformer implements BytecodeTransformer {

  private final NameMapping mapping;

  public MappingBytecodeTransformer(@Nonnull NameMapping mapping) {
    this.mapping = mapping;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    // since we don't know whether a class contains references to a type we're mapping until we've
    // visited it, we'll have to transform all classes
    return Optional.of(new ClassRemapper(nextVisitor, new DelegatingMapper()));
  }

  /**
   * Provides a remapper implementation which delegates all of its queries to one or more {@link
   * org.basinmc.plunger.common.mapping} interfaces.
   */
  private class DelegatingMapper extends Remapper {

    /**
     * {@inheritDoc}
     */
    @Override
    public String mapType(String type) {
      return MappingBytecodeTransformer.this.mapping.getClassName(type)
          .orElse(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String mapMethodName(String owner, String name, String desc) {
      // since we cannot actually map the names of constructors, we'll simply skip them entirely
      // here to prevent any issues due to badly designed mapping implementations
      if ("<init>".equals(name)) {
        return name;
      }

      return MappingBytecodeTransformer.this.mapping.getMethodName(owner, name, desc)
          .orElse(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String mapFieldName(String owner, String name, String desc) {
      return MappingBytecodeTransformer.this.mapping.getFieldName(owner, name, desc)
          .orElse(name);
    }
  }
}
