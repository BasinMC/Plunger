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
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.file.Path;
import java.util.Optional;
import org.basinmc.plunger.mapping.NameMapping;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Provides a bytecode transformer which remaps the names of classes based on a pre-configured set
 * of mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class NameMappingBytecodeTransformer implements BytecodeTransformer {

  private final NameMapping mapping;

  public NameMappingBytecodeTransformer(@NonNull NameMapping mapping, boolean overrideParameters) {
    this.mapping = mapping;
  }

  public NameMappingBytecodeTransformer(@NonNull NameMapping mapping) {
    this(mapping, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    // since we don't know whether a class contains references to a type we're mapping until we've
    // visited it, we'll have to transform all classes
    return Optional.of(new ExtendedClassRemapper(nextVisitor, new DelegatingMapper()));
  }

  /**
   * Provides a remapper implementation which delegates all of its queries to one or more {@link
   * org.basinmc.plunger.mapping} interfaces.
   */
  private class DelegatingMapper extends Remapper {

    /**
     * {@inheritDoc}
     */
    @Override
    public String map(@NonNull String type) {
      return NameMappingBytecodeTransformer.this.mapping.getClassName(type)
          .orElse(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String mapFieldName(String owner, String name, String desc) {
      return NameMappingBytecodeTransformer.this.mapping.getFieldName(owner, name, desc)
          .orElse(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String mapMethodName(String owner, String name, String desc) {
      // since we cannot actually map the names of constructors, we'll simply skip them entirely
      // here to prevent any issues due to badly designed mapping implementations
      if ("<init>".equals(name) || "<clinit>".equals(name)) {
        return name;
      }

      return NameMappingBytecodeTransformer.this.mapping.getMethodName(owner, name, desc)
          .orElse(name);
    }
  }

  /**
   * Provides an extended implementation of {@link ClassRemapper} which also correctly alters the
   * names of inner class names.
   */
  private final class ExtendedClassRemapper extends ClassRemapper {

    private ExtendedClassRemapper(@NonNull ClassVisitor cv, @NonNull Remapper remapper) {
      super(cv, remapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      return this.createMethodRemapper(
          super.visitMethod(access, name, desc, signature, exceptions),
          name,
          desc
      );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
      if (innerName != null) {
        String tmp = this.remapper.mapType(name);
        int innerSeparator = tmp.lastIndexOf('$');

        if (innerSeparator != -1) {
          innerName = tmp.substring(innerSeparator + 1);
        }
      }

      super.visitInnerClass(name, outerName, innerName, access);
    }

    @NonNull
    protected MethodVisitor createMethodRemapper(@Nullable MethodVisitor mv, String methodName,
        String methodDescriptor) {
      return new ExtendedMethodRemapper(
          this.createMethodRemapper(mv),
          this.remapper,
          this.className,
          methodName,
          methodDescriptor
      );
    }
  }

  private final class ExtendedMethodRemapper extends MethodRemapper {

    private final String className;
    private final String methodName;
    private final String descriptor;

    private final Type[] parameters;

    private ExtendedMethodRemapper(
        @NonNull MethodVisitor mv,
        @NonNull Remapper remapper,
        @NonNull String className,
        @NonNull String methodName,
        @NonNull String descriptor) {
      super(mv, remapper);
      this.className = className;
      this.methodName = methodName;
      this.descriptor = descriptor;

      this.parameters = Type.getArgumentTypes(descriptor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start,
        Label end, int index) {
      if (index > 0 && index <= this.parameters.length) {
        name = NameMappingBytecodeTransformer.this.mapping
            .getParameterName(this.className, this.methodName, this.descriptor, name, index - 1)
            .orElse(name);
      }

      super.visitLocalVariable(name, desc, signature, start, end, index);
    }
  }
}
