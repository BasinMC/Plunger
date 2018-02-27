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
import java.util.Optional;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Provides a class visitor implementation which is capable of dynamically shifting its follow-up
 * visitor.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class DelegatingClassVisitor extends ClassVisitor {

  public DelegatingClassVisitor(@Nullable ClassVisitor visitor) {
    super(Opcodes.ASM6, visitor);
  }

  public DelegatingClassVisitor() {
    this(null);
  }

  @NonNull
  public Optional<ClassVisitor> getVisitor() {
    return Optional.ofNullable(this.cv);
  }

  public void setVisitor(@Nullable ClassVisitor visitor) {
    this.cv = visitor;
  }
}
