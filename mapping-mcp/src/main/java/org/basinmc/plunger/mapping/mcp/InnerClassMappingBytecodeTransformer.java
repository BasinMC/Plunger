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
import org.objectweb.asm.Opcodes;

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
  public Optional<ClassVisitor> createTransformer(@NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    return Optional.of(new InnerClassMappingClassVisitor(nextVisitor));
  }

  private final class InnerClassMappingClassVisitor extends ClassVisitor {

    private InnerClassMappingClassVisitor(@NonNull ClassVisitor classVisitor) {
      super(Opcodes.ASM6, classVisitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);

      InnerClassMappingBytecodeTransformer.this.mapping.getMapping(name)
          .ifPresent((e) -> {
            e.getEnclosingMethod()
                .ifPresent(
                    (enclosure) -> this.visitOuterClass(enclosure.getOwner(), enclosure.getName(),
                        enclosure.getDescriptor()));

            e.getInnerClasses().forEach((cl) -> this
                .visitInnerClass(cl.getInnerClass(), cl.getOuterClass(), cl.getInnerName(),
                    cl.getAccess()));
          });
    }
  }
}
