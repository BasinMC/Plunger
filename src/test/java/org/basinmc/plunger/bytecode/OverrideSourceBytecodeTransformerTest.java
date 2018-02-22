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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.basinmc.plunger.AbstractPlungerTest;
import org.basinmc.plunger.BytecodePlunger;
import org.basinmc.plunger.Plunger;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Evaluates whether {@link OverrideSourceBytecodeTransformer} operates within its bounds.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class OverrideSourceBytecodeTransformerTest extends AbstractPlungerTest {

  /**
   * Evaluates whether the transformer correctly replaces all source code occurrences with the
   * provided static values.
   */
  @Test
  public void testExecute() throws IOException {
    Path testFile = Paths.get("TestClass.class");
    this.extractSourceFile("/TestClass.class", testFile);

    BytecodePlunger plunger = Plunger.bytecodeBuilder()
        .withTransformer(new OverrideSourceBytecodeTransformer("SourceFile.java", 42))
        .build(this.getSource(), this.getTarget());
    plunger.apply();

    try (InputStream inputStream = Files.newInputStream(this.getTarget().resolve(testFile))) {
      ClassReader reader = new ClassReader(inputStream);
      reader.accept(new ValidationClassVisitor(), ClassReader.EXPAND_FRAMES);
    }
  }

  private static final class ValidationClassVisitor extends ClassVisitor {

    private ValidationClassVisitor() {
      super(Opcodes.ASM6);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      return new OverrideSourceBytecodeTransformerTest.ValidationMethodVisitor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitSource(String source, String debug) {
      Assert.assertEquals("SourceFile.java", source);
    }
  }

  private static final class ValidationMethodVisitor extends MethodVisitor {

    private ValidationMethodVisitor() {
      super(Opcodes.ASM6);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLineNumber(int line, Label start) {
      Assert.assertEquals(42, line);
    }
  }
}
