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
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import org.basinmc.plunger.bytecode.transformer.BytecodeTransformer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Changes the access level of methods to be equal to the most permissive level within the
 * inheritance chain in order to regain source code compatibility on optimized classes (e.g. classes
 * which have been passed through a program like Proguard).
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class AccessLevelCorrectionBytecodeTransformer implements BytecodeTransformer {

  private static final Logger logger = LoggerFactory
      .getLogger(AccessLevelCorrectionBytecodeTransformer.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ClassVisitor> createTransformer(@NonNull Context context, @NonNull Path source,
      @NonNull ClassVisitor nextVisitor) {
    return Optional
        .of(new AccessLevelCorrectionClassVisitor(nextVisitor, context.getClassMetadata()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean usesClassMetadata() {
    return true;
  }

  /**
   * Changes the access level of methods to be equal to the most permissive level within the
   * inheritance chain.
   */
  private static final class AccessLevelCorrectionClassVisitor extends ClassVisitor {

    private final ClassMetadata metadata;
    private String className;

    private AccessLevelCorrectionClassVisitor(@NonNull ClassVisitor classVisitor,
        @NonNull ClassMetadata metadata) {
      super(Opcodes.ASM6, classVisitor);
      this.metadata = metadata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
      this.className = name;
      super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
        String[] exceptions) {
      if ("<init>".equals(name) || "<clinit>".equals(name)) {
        return super.visitMethod(access, name, descriptor, signature, exceptions);
      }

      int newAccess = this.metadata.walkInheritanceTree(this.className)
          .flatMap((o) -> this.metadata.getMethodAccess(o, name, descriptor)
              .map(Stream::of)
              .orElseGet(Stream::empty))
          .map((v) -> v & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE))
          .min(Comparator.comparingInt((v) -> {
            // since Java only permits us to mark access to a certain method less restrictive than
            // its parent, we'll remap the standard access values in order of their restriction
            // level
            switch (v) {
              case Opcodes.ACC_PRIVATE:
                return 3;
              case Opcodes.ACC_PROTECTED:
                return 2;
              case Opcodes.ACC_PUBLIC:
                return 0;
              default:
                return 1;
            }
          }))
          .map((v) -> (access & ~0x7) ^ v)
          .orElse(access);

      if (access != newAccess) {
        logger.debug("  Mapping access for method {}#{}{} from 0x{} to 0x{}", this.className, name,
            descriptor, Integer.toHexString(access), Integer.toHexString(newAccess));
      }

      return super.visitMethod(newAccess, name, descriptor, signature, exceptions);
    }
  }
}
