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
package org.basinmc.plunger.mapping.mcp;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.basinmc.plunger.bytecode.transformer.BytecodeTransformer;
import org.basinmc.plunger.mapping.mcp.InnerClassMapping.InnerClass;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Provides a bytecode transformer which restores the inner class relationship using a JSON based
 * map of inner class types.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class InnerClassMappingBytecodeTransformer implements BytecodeTransformer {

  private final InnerClassMapping mapping;

  public InnerClassMappingBytecodeTransformer(@NonNull InnerClassMapping mapping) {
    this.mapping = mapping;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Context context, @NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    return Optional.of(new InnerClassMappingClassVisitor(nextVisitor));
  }

  private final class InnerClassMappingClassVisitor extends ClassVisitor {

    private String className;
    private boolean visitedOuterType;

    private final Set<String> visitedInnerTypes = new HashSet<>();
    private final Set<String> referencedInnerTypes = new HashSet<>();

    private InnerClassMappingClassVisitor(@NonNull ClassVisitor classVisitor) {
      super(Opcodes.ASM6, classVisitor);
    }

    private boolean isInnerType(@NonNull String name) {
      return name.contains("$");
    }

    private void reference(@NonNull Type type) {
      if (type.getSort() == Type.ARRAY) {
        type = type.getElementType();
      }

      if (type.getSort() == Type.OBJECT) {
        String name = type.getInternalName();

        if (this.isInnerType(name)) {
          this.referencedInnerTypes.add(name);
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
      this.className = name;

      Stream.of(interfaces)
          .filter(this::isInnerType)
          .forEach(this.referencedInnerTypes::add);

      super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
      InnerClassMappingBytecodeTransformer.this.mapping.getMapping(this.className)
          .ifPresent((e) -> {
            e.getEnclosingMethod()
                .filter((enclosure) ->
                    !this.visitedOuterType && enclosure.getName() != null
                        && enclosure.getDescriptor() != null)
                .ifPresent(
                    (enclosure) -> super.visitOuterClass(enclosure.getOwner(), enclosure.getName(),
                        enclosure.getDescriptor()));

            e.getInnerClasses().stream()
                .filter((cl) -> !this.visitedInnerTypes.contains(cl.getInnerClass()))
                .forEach((cl) -> {
                  this.visitedInnerTypes.add(cl.getInnerClass());
                  super.visitInnerClass(cl.getInnerClass(), cl.getOuterClass(), cl.getInnerName(),
                      cl.getAccess());
                });
          });

      this.referencedInnerTypes.removeAll(this.visitedInnerTypes);

      this.referencedInnerTypes.forEach((cl) -> {
        InnerClass innerClass = InnerClassMappingBytecodeTransformer.this.mapping.getInnerClass(cl)
            .orElse(null);

        if (innerClass == null) {
          int lastInnerSeparator = cl.lastIndexOf('$');

          String outerName = cl.substring(0, lastInnerSeparator);
          String innerName = cl.substring(lastInnerSeparator + 1);

          super.visitInnerClass(cl, outerName, innerName, Opcodes.ACC_PUBLIC);
          return;
        }

        super.visitInnerClass(innerClass.getInnerClass(), innerClass.getOuterClass(),
            innerClass.getInnerName(), innerClass.getAccess());
      });

      super.visitEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature,
        Object value) {
      this.reference(Type.getType(descriptor));

      return super.visitField(access, name, descriptor, signature, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
      this.visitedInnerTypes.add(name);
      super.visitInnerClass(name, outerName, innerName, access);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
        String[] exceptions) {
      if (exceptions != null) {
        Stream.of(exceptions)
            .filter(this::isInnerType)
            .forEach(this.referencedInnerTypes::add);
      }

      this.reference(Type.getType(descriptor));

      for (Type parameterType : Type.getArgumentTypes(descriptor)) {
        this.reference(parameterType);
      }

      MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);

      return new MethodVisitor(Opcodes.ASM6, visitor) {
        @Override
        public void visitLdcInsn(Object value) {
          if (value instanceof Type) {
            InnerClassMappingClassVisitor.this.reference((Type) value);
          }

          super.visitLdcInsn(value);
        }
      };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
      this.visitedOuterType = true;
      super.visitOuterClass(owner, name, descriptor);
    }
  }
}
