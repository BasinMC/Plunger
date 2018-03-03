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
import org.basinmc.plunger.mapping.AccessFlag;
import org.basinmc.plunger.mapping.AccessMapping;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Provides a bytecode transformer which alters the access level of classes, fields and methods
 * based on a pre-defined mapping.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class AccessMappingBytecodeTransformer implements BytecodeTransformer {

  private final AccessMapping accessMapping;

  public AccessMappingBytecodeTransformer(@NonNull AccessMapping accessMapping) {
    this.accessMapping = accessMapping;
  }

  @NonNull
  private static AccessFlag byOpcode(int opcode) {
    AccessFlag flag = AccessFlag.PACKAGE_PRIVATE;

    if ((opcode & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE) {
      flag = flag.add(AccessFlag.PRIVATE);
    } else if ((opcode & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED) {
      flag = flag.add(AccessFlag.PROTECTED);
    } else if ((opcode & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC) {
      flag = flag.add(AccessFlag.PUBLIC);
    }

    if ((opcode & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
      flag = flag.add(AccessFlag.FINAL);
    }

    return flag;
  }

  private static int toOpcode(@NonNull AccessFlag flag) {
    int opcode = 0;

    if (flag.contains(AccessFlag.PRIVATE)) {
      opcode |= Opcodes.ACC_PRIVATE;
    } else if (flag.contains(AccessFlag.PROTECTED)) {
      opcode |= Opcodes.ACC_PROTECTED;
    } else if (flag.contains(AccessFlag.PUBLIC)) {
      opcode |= Opcodes.ACC_PUBLIC;
    }

    if (flag.contains(AccessFlag.FINAL)) {
      opcode |= Opcodes.ACC_FINAL;
    }

    return opcode;
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Context context, @NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    return Optional.of(new AccessOverrideClassVisitor(nextVisitor));
  }

  /**
   * Resolves the access level of all visited classes, fields and methods.
   */
  private final class AccessOverrideClassVisitor extends ClassVisitor {

    private String enclosingClass;

    private AccessOverrideClassVisitor(@NonNull ClassVisitor classVisitor) {
      super(Opcodes.ASM6, classVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(int version, int access, @NonNull String name,
        String signature,
        String superName,
        String[] interfaces) {
      this.enclosingClass = name;

      access = AccessMappingBytecodeTransformer.this.accessMapping
          .getClassAccessFlags(name, byOpcode(access))
          .map(AccessMappingBytecodeTransformer::toOpcode)
          .orElse(access);

      super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldVisitor visitField(int access, @NonNull String name, @NonNull String descriptor,
        String signature,
        Object value) {
      access = AccessMappingBytecodeTransformer.this.accessMapping
          .getFieldAccessFlags(this.enclosingClass, name, descriptor, byOpcode(access))
          .map(AccessMappingBytecodeTransformer::toOpcode)
          .orElse(access);

      return super.visitField(access, name, descriptor, signature, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
        String[] exceptions) {
      access = AccessMappingBytecodeTransformer.this.accessMapping
          .getMethodAccessFlags(this.enclosingClass, name, descriptor, byOpcode(access))
          .map(AccessMappingBytecodeTransformer::toOpcode)
          .orElse(access);

      return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
  }
}
