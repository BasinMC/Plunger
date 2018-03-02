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
import java.util.Optional;
import org.basinmc.plunger.bytecode.transformer.BytecodeTransformer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Provides a transformer which will add missing inner class constructors.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class InnerClassConstructorBytecodeTransformer implements BytecodeTransformer {

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    return Optional.of(new InnerClassConstructionClassVisitor(nextVisitor));
  }

  private static class InnerClassConstructionClassVisitor extends ClassVisitor {

    private String className;
    private String parentName;
    private String parentField;

    private boolean hasConstructor;
    private boolean isStatic;

    private InnerClassConstructionClassVisitor(@NonNull ClassVisitor classVisitor) {
      super(Opcodes.ASM6, classVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
      this.className = name;
      this.isStatic = (access & Opcodes.ACC_STATIC) != 0;

      super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
      if (!this.hasConstructor && !this.isStatic && this.parentName != null
          && this.parentField != null) {
        MethodVisitor visitor = this.visitMethod(
            Opcodes.ACC_PRIVATE ^ Opcodes.ACC_SYNTHETIC,
            "<init>",
            "(" + this.parentName + ")V",
            null,
            null
        );

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, this.className, this.parentField, this.parentName);
        visitor.visitInsn(Opcodes.RETURN);
      }

      super.visitEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature,
        Object value) {
      if ((access & Opcodes.ACC_SYNTHETIC) != 0 && (access & Opcodes.ACC_FINAL) != 0 && descriptor
          .equals(this.parentName)) {
        this.parentField = name;
      }

      return super.visitField(access, name, descriptor, signature, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
      if (this.className.equals(name)) {
        this.parentName = "L" + outerName + ";";
      }

      super.visitInnerClass(name, outerName, innerName, access);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
        String[] exceptions) {
      if ("<init>".equals(name)) {
        this.hasConstructor = true;
      }

      return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
  }
}
