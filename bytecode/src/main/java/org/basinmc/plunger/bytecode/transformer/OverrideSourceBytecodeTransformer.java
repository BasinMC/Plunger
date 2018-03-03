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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Overrides all source code related attributes within transformed classes with a set value.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class OverrideSourceBytecodeTransformer implements BytecodeTransformer {

  private final Integer line;
  private final String source;

  public OverrideSourceBytecodeTransformer(@Nullable String source, @Nullable Integer line) {
    if (source == null && line == null) {
      throw new IllegalArgumentException(
          "Illegal override configuration: Must specify either source or line");
    }

    this.source = source;
    this.line = line;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Context context, @NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    return Optional.of(new OverridingClassVisitor(nextVisitor));
  }

  /**
   * Replaces any occurrences of the source and line number attributes with static values.
   */
  private final class OverridingClassVisitor extends ClassVisitor {

    private OverridingClassVisitor(@NonNull ClassVisitor classVisitor) {
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

      return new OverridingMethodVisitor(visitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitSource(String source, String debug) {
      String override = OverrideSourceBytecodeTransformer.this.source;
      super.visitSource(override != null ? override : source, debug);
    }
  }

  /**
   * Replaces any occurrences of the source and line number attributes with static values.
   */
  private final class OverridingMethodVisitor extends MethodVisitor {

    private OverridingMethodVisitor(@NonNull MethodVisitor methodVisitor) {
      super(Opcodes.ASM6, methodVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLineNumber(int line, Label start) {
      Integer override = OverrideSourceBytecodeTransformer.this.line;
      super.visitLineNumber(override != null ? override : line, start);
    }
  }
}
