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
import edu.umd.cs.findbugs.annotations.Nullable;
import org.basinmc.plunger.mapping.NameMapping;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Provides a bytecode transformer which remaps the names of classes based on a pre-configured set
 * of mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class NameMappingBytecodeTransformer implements BytecodeTransformer {

  private final NameMapping mapping;
  private final boolean overrideParameters;

  public NameMappingBytecodeTransformer(@NonNull NameMapping mapping, boolean overrideParameters) {
    this.mapping = mapping;
    this.overrideParameters = overrideParameters;
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
    return Optional
        .of(new ClassRemapper(new ParameterNameClassVisitor(nextVisitor), new DelegatingMapper()));
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
      if ("<init>".equals(name)) {
        return name;
      }

      return NameMappingBytecodeTransformer.this.mapping.getMethodName(owner, name, desc)
          .orElse(name);
    }
  }

  /**
   * Provides a class visitor which will remap the names of parameters.
   */
  private final class ParameterNameClassVisitor extends ClassVisitor {

    private String className;

    private ParameterNameClassVisitor(@NonNull ClassVisitor classVisitor) {
      super(Opcodes.ASM6, classVisitor);
    }

    @Override
    public void visit(int version, int access, @NonNull String name, String signature,
        String superName,
        String[] interfaces) {
      this.className = name;
      super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(int access, @NonNull String name, @NonNull String descriptor,
        @Nullable String signature, @Nullable String[] exceptions) {
      MethodVisitor visitor = super.visitMethod(access, name, descriptor, descriptor, exceptions);

      if (visitor == null) {
        return null;
      }

      visitor = new ParameterNameMethodVisitor(visitor, this.className, name, descriptor);

      if (NameMappingBytecodeTransformer.this.overrideParameters) {
        Type[] arguments = Type.getArgumentTypes(descriptor);
        int i = 0;

        for (Type argument : arguments) {
          visitor.visitParameter("param" + (i++), 0);
        }

        return new ParameterConsumerMethodVisitor(visitor);
      }

      return visitor;
    }
  }

  private static final class ParameterConsumerMethodVisitor extends MethodVisitor {

    private ParameterConsumerMethodVisitor(@NonNull MethodVisitor methodVisitor) {
      super(Opcodes.ASM6, methodVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitParameter(String name, int access) {
    }
  }

  /**
   * Provides a method visitor which will remap the names of parameters.
   */
  private final class ParameterNameMethodVisitor extends MethodVisitor {

    private final String className;
    private final String descriptor;
    private final String methodName;
    private int parameterIndex;

    private ParameterNameMethodVisitor(
        @NonNull MethodVisitor methodVisitor,
        @NonNull String className,
        @NonNull String methodName,
        @NonNull String descriptor) {
      super(Opcodes.ASM6, methodVisitor);
      this.className = className;
      this.methodName = methodName;
      this.descriptor = descriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitParameter(@Nullable String name, int access) {
      // TODO: Evaluate whether name is actually passed as null when LVT is missing
      name = NameMappingBytecodeTransformer.this.mapping
          .getParameterName(this.className, this.methodName, this.descriptor, name,
              this.parameterIndex++).orElse(name);

      super.visitParameter(name, access);
    }
  }
}
