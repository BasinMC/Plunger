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
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Provides a Bytecode transformer which deletes arbitrary debug information from all of its
 * processed classes.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class DebugAttributeBytecodeTransformer implements BytecodeTransformer {

  private final boolean removeSourceCode;
  private final boolean removeLineNumber;
  private final boolean removeGenerics;
  private final boolean removeLocalVariableTable;

  public DebugAttributeBytecodeTransformer(
      boolean removeSourceCode,
      boolean removeLineNumber,
      boolean removeGenerics,
      boolean removeLocalVariableTable) {
    this.removeSourceCode = removeSourceCode;
    this.removeLineNumber = removeLineNumber;
    this.removeGenerics = removeGenerics;
    this.removeLocalVariableTable = removeLocalVariableTable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    return Optional.of(new DebugClassAttributeVisitor(nextVisitor));
  }

  /**
   * Deletes the local variable table, the line number table and generics attributes from classes
   * depending on the transformer configuration.
   */
  private final class DebugClassAttributeVisitor extends ClassVisitor {

    private DebugClassAttributeVisitor(@Nonnull ClassVisitor classVisitor) {
      super(Opcodes.ASM6, classVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitAttribute(@Nonnull Attribute attribute) {
      if (DebugAttributeBytecodeTransformer.this.removeLocalVariableTable && "LocalVariableTable"
          .equals(attribute.type)) {
        return;
      }

      if (DebugAttributeBytecodeTransformer.this.removeGenerics && "LocalVariableTypeTable"
          .equals(attribute.type)) {
        return;
      }

      super.visitAttribute(attribute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature,
        Object value) {
      FieldVisitor visitor = super.visitField(access, name, descriptor, signature, value);

      if (visitor == null) {
        return null;
      }

      return new DebugFieldAttributeVisitor(visitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
        String[] exceptions) {
      MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);

      if (visitor == null) {
        return null;
      }

      return new DebugMethodAttributeVisitor(visitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitSource(String source, String debug) {
      if (!DebugAttributeBytecodeTransformer.this.removeSourceCode) {
        super.visitSource(source, debug);
      }
    }
  }

  /**
   * Deletes the local variable table, the line number table and generics attributes from methods
   * depending on the transformer configuration.
   */
  private final class DebugFieldAttributeVisitor extends FieldVisitor {

    private DebugFieldAttributeVisitor(@Nonnull FieldVisitor fieldVisitor) {
      super(Opcodes.ASM6, fieldVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitAttribute(Attribute attribute) {
      if (DebugAttributeBytecodeTransformer.this.removeLocalVariableTable && "LocalVariableTable"
          .equals(attribute.type)) {
        return;
      }

      if (DebugAttributeBytecodeTransformer.this.removeGenerics && "LocalVariableTypeTable"
          .equals(attribute.type)) {
        return;
      }

      super.visitAttribute(attribute);
    }
  }

  /**
   * Deletes the local variable table, the line number table and generics attributes from fields
   * depending on the transformer configuration.
   */
  private final class DebugMethodAttributeVisitor extends MethodVisitor {

    private DebugMethodAttributeVisitor(MethodVisitor methodVisitor) {
      super(Opcodes.ASM6, methodVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitAttribute(Attribute attribute) {
      if (DebugAttributeBytecodeTransformer.this.removeLocalVariableTable && "LocalVariableTable"
          .equals(attribute.type)) {
        return;
      }

      if (DebugAttributeBytecodeTransformer.this.removeGenerics && "LocalVariableTypeTable"
          .equals(attribute.type)) {
        return;
      }

      super.visitAttribute(attribute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLineNumber(int line, Label start) {
      if (!DebugAttributeBytecodeTransformer.this.removeLineNumber) {
        super.visitLineNumber(line, start);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start,
        Label end, int index) {
      if (!DebugAttributeBytecodeTransformer.this.removeLocalVariableTable) {
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
      }
    }
  }
}
