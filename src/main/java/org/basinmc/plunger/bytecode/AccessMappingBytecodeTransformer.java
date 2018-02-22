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
import org.basinmc.plunger.common.mapping.AccessFlag;
import org.basinmc.plunger.common.mapping.AccessMapping;
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

  public AccessMappingBytecodeTransformer(@Nonnull AccessMapping accessMapping) {
    this.accessMapping = accessMapping;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    return Optional.of(new AccessOverrideClassVisitor(nextVisitor));
  }

  /**
   * Resolves the access level of all visited classes, fields and methods.
   */
  private final class AccessOverrideClassVisitor extends ClassVisitor {

    private String enclosingClass;

    private AccessOverrideClassVisitor(@Nonnull ClassVisitor classVisitor) {
      super(Opcodes.ASM6, classVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(int version, int access, @Nonnull String name,
        String signature,
        String superName,
        String[] interfaces) {
      this.enclosingClass = name;

      access = AccessMappingBytecodeTransformer.this.accessMapping
          .getClassAccessFlags(name, AccessFlag.byOpcode(access))
          .map(AccessFlag::toOpcode)
          .orElse(access);

      super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldVisitor visitField(int access, @Nonnull String name, @Nonnull String descriptor,
        String signature,
        Object value) {
      access = AccessMappingBytecodeTransformer.this.accessMapping
          .getFieldAccessFlags(this.enclosingClass, name, descriptor, AccessFlag.byOpcode(access))
          .map(AccessFlag::toOpcode)
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
          .getMethodAccessFlags(this.enclosingClass, name, descriptor, AccessFlag.byOpcode(access))
          .map(AccessFlag::toOpcode)
          .orElse(access);

      return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
  }
}
