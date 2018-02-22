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
import org.junit.Test;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Evaluates whether {@link DebugAttributeBytecodeTransformer} works as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class DebugAttributeBytecodeTransformerTest extends AbstractPlungerTest {

  /**
   * Evaluates whether the transformer correctly deletes all debug information from the test class
   * when instructed to do so.
   */
  @Test
  public void testExecute() throws IOException {
    Path testFile = Paths.get("TestClass.class");
    this.extractSourceFile("/TestClass.class", testFile);

    BytecodePlunger plunger = Plunger.bytecodeBuilder()
        .withTransformer(new DebugAttributeBytecodeTransformer(true, true, true, true))
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
    public void visitAttribute(Attribute attr) {
      if ("LocalVariableTable".equals(attr.type)) {
        throw new AssertionError("Encountered LocalVariableTable");
      }

      if ("LocalVariableTypeTable".equals(attr.type)) {
        throw new AssertionError("Encountered LocalVariableTypeTable");
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature,
        Object value) {
      return new ValidationFieldVisitor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      return new ValidationMethodVisitor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitSource(String source, String debug) {
      throw new AssertionError("Encountered Source");
    }
  }

  private static final class ValidationFieldVisitor extends FieldVisitor {

    private ValidationFieldVisitor() {
      super(Opcodes.ASM6);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitAttribute(Attribute attr) {
      if ("LocalVariableTable".equals(attr.type)) {
        throw new AssertionError("Encountered LocalVariableTable");
      }

      if ("LocalVariableTypeTable".equals(attr.type)) {
        throw new AssertionError("Encountered LocalVariableTypeTable");
      }
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
    public void visitAttribute(Attribute attr) {
      if ("LocalVariableTable".equals(attr.type)) {
        throw new AssertionError("Encountered LocalVariableTable");
      }

      if ("LocalVariableTypeTable".equals(attr.type)) {
        throw new AssertionError("Encountered LocalVariableTypeTable");
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLineNumber(int line, Label start) {
      throw new AssertionError("Encountered LineNumber");
    }
  }
}
