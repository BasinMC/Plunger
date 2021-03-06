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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.basinmc.plunger.bytecode.BytecodePlunger;
import org.basinmc.plunger.test.AbstractPlungerTest;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Evaluates whether {@link OverrideLocalVariableBytecodeTransformer} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class OverrideLocalVariableBytecodeTransformerTest extends AbstractPlungerTest {

  /**
   * Evaluates whether the transformer correctly replaces all variable names.
   */
  @Test
  public void testApply() throws IOException {
    Path testFile = Paths.get("TestClass.class");
    this.extractSourceFile("/TestClass.bytecode", testFile);

    BytecodePlunger plunger = BytecodePlunger.builder()
        .withSourceRelocation(false)
        .withTransformer(new OverrideLocalVariableBytecodeTransformer("☃"))
        .build(this.getSource(), this.getTarget());
    plunger.apply();

    try (InputStream inputStream = Files.newInputStream(this.getTarget().resolve(testFile))) {
      ClassReader reader = new ClassReader(inputStream);
      ValidationClassVisitor validationVisitor = new ValidationClassVisitor();
      reader.accept(validationVisitor, ClassReader.EXPAND_FRAMES);
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
      return new OverrideLocalVariableBytecodeTransformerTest.ValidationMethodVisitor();
    }
  }

  private static final class ValidationMethodVisitor extends MethodVisitor {

    private ValidationMethodVisitor() {
      super(Opcodes.ASM6);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start,
        Label end, int index) {
      Assert.assertEquals("☃", name);
    }
  }
}
