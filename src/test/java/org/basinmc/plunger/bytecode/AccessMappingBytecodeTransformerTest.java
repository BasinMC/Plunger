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
import java.util.Optional;
import org.basinmc.plunger.AbstractPlungerTest;
import org.basinmc.plunger.BytecodePlunger;
import org.basinmc.plunger.Plunger;
import org.basinmc.plunger.common.mapping.AccessFlag;
import org.basinmc.plunger.common.mapping.AccessMapping;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class AccessMappingBytecodeTransformerTest extends AbstractPlungerTest {

  @Test
  public void testExecute() throws IOException {
    Path testFile = Paths.get("TestClass.class");
    this.extractSourceFile("/TestClass.bytecode", testFile);

    AccessMapping mapping = Mockito.mock(AccessMapping.class);

    Mockito
        .when(mapping.getClassAccessFlags("org/basinmc/plunger/test/TestClass", AccessFlag.PUBLIC))
        .thenReturn(Optional.of(AccessFlag.PACKAGE_PRIVATE));
    Mockito.when(mapping.getFieldAccessFlags("org/basinmc/plunger/test/TestClass", "testField", "I",
        AccessFlag.PRIVATE.add(AccessFlag.FINAL)))
        .thenReturn(Optional.of(AccessFlag.PUBLIC));
    Mockito.when(mapping
        .getMethodAccessFlags("org/basinmc/plunger/test/TestClass", "testMethod", "()I",
            AccessFlag.PUBLIC))
        .thenReturn(Optional.of(AccessFlag.PROTECTED.add(AccessFlag.FINAL)));

    BytecodePlunger plunger = Plunger.bytecodeBuilder()
        .withTransformer(new AccessMappingBytecodeTransformer(mapping))
        .build(this.getSource(), this.getTarget());
    plunger.apply();

    ClassNode node = new ClassNode();

    try (InputStream inputStream = Files.newInputStream(this.getTarget().resolve(testFile))) {
      ClassReader reader = new ClassReader(inputStream);
      reader.accept(node, ClassReader.EXPAND_FRAMES);
    }

    FieldNode fieldNode = node.fields.stream()
        .filter((n) -> "testField".equals(n.name))
        .findAny()
        .orElseThrow(() -> new AssertionError("No such field: testField"));

    MethodNode methodNode = node.methods.stream()
        .filter((n) -> "testMethod".equals(n.name))
        .findAny()
        .orElseThrow(() -> new AssertionError("No such method: testMethod"));

    Assert.assertEquals(0, node.access);
    Assert.assertEquals(Opcodes.ACC_PUBLIC, fieldNode.access);
    Assert.assertEquals(Opcodes.ACC_PROTECTED ^ Opcodes.ACC_FINAL, methodNode.access);
  }
}
