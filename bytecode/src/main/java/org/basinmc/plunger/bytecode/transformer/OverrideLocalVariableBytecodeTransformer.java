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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Overrides the name of all local variables within any transformed class.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class OverrideLocalVariableBytecodeTransformer implements BytecodeTransformer {

  private final String name;

  public OverrideLocalVariableBytecodeTransformer(@NonNull String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Context context, @NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    return Optional.of(new OverrideLocalVariableClassVisitor(nextVisitor));
  }

  private final class OverrideLocalVariableClassVisitor extends ClassVisitor {

    private OverrideLocalVariableClassVisitor(ClassVisitor classVisitor) {
      super(Opcodes.ASM6, classVisitor);
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

      return new OverrideLocalVariableMethodVisitor(visitor);
    }
  }

  private final class OverrideLocalVariableMethodVisitor extends MethodVisitor {

    private OverrideLocalVariableMethodVisitor(MethodVisitor methodVisitor) {
      super(Opcodes.ASM6, methodVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start,
        Label end, int index) {
      super.visitLocalVariable(OverrideLocalVariableBytecodeTransformer.this.name, descriptor,
          signature,
          start, end, index);
    }
  }
}
