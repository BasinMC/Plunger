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
import org.basinmc.plunger.common.mapping.ClassNameMapping;
import org.basinmc.plunger.common.mapping.DelegatingNameMapping;
import org.basinmc.plunger.common.mapping.FieldNameMapping;
import org.basinmc.plunger.common.mapping.MethodNameMapping;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Evaluates whether {@link NameMappingBytecodeTransformer} performs as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class NameMappingBytecodeTransformerTest extends AbstractPlungerTest {

  /**
   * Evaluates whether the transformer provides the expected results.
   */
  @Test
  public void testExecute() throws IOException {
    Path testFile = Paths.get("TestClass.class");
    this.extractSourceFile("/TestClass.bytecode", testFile);

    ClassNameMapping classMapping = Mockito.mock(ClassNameMapping.class);
    FieldNameMapping fieldMapping = Mockito.mock(FieldNameMapping.class);
    MethodNameMapping methodMapping = Mockito.mock(MethodNameMapping.class);

    Mockito.when(classMapping.getClassName("org/basinmc/plunger/test/TestClass"))
        .thenReturn(Optional.of("org/basinmc/plunger/mapped/test/MappedTestClass"));
    Mockito.when(fieldMapping
        .getFieldName("org/basinmc/plunger/mapped/test/MappedTestClass", "testField", "I"))
        .thenReturn(Optional.of("a"));
    Mockito.when(methodMapping
        .getMethodName("org/basinmc/plunger/mapped/test/MappedTestClass", "testMethod", "()I"))
        .thenReturn(Optional.of("a"));

    BytecodePlunger plunger = Plunger.bytecodeBuilder()
        .withSourceRelocation(false)
        .withTransformer(new NameMappingBytecodeTransformer(
            DelegatingNameMapping.builder()
                .withClassMapping(classMapping)
                .withFieldNameMapping(fieldMapping)
                .withMethodNameMapping(methodMapping)
                .withResolveEnclosure()
                .build()
        ))
        .build(this.getSource(), this.getTarget());
    plunger.apply();

    Mockito.verify(classMapping, Mockito.times(10))
        .getClassName("org/basinmc/plunger/test/TestClass");
    Mockito.verify(fieldMapping, Mockito.times(2))
        .getFieldName("org/basinmc/plunger/mapped/test/MappedTestClass", "testField", "I");
    Mockito.verify(methodMapping, Mockito.times(2))
        .getMethodName("org/basinmc/plunger/mapped/test/MappedTestClass", "testMethod", "()I");

    ClassNode node = new ClassNode();

    try (InputStream inputStream = Files.newInputStream(this.getTarget().resolve(testFile))) {
      ClassReader reader = new ClassReader(inputStream);
      reader.accept(node, ClassReader.EXPAND_FRAMES);
    }

    Assert.assertEquals("org/basinmc/plunger/mapped/test/MappedTestClass", node.name);
    Assert.assertEquals(3, node.methods.size());

    FieldNode fieldNode = node.fields.stream()
        .filter((f) -> "a".equals(f.name))
        .findAny()
        .orElseThrow(() -> new AssertionError("No such field: a"));
    Assert.assertEquals("I", fieldNode.desc);

    MethodNode methodNode = node.methods.stream()
        .filter((m) -> "a".equals(m.name))
        .findAny()
        .orElseThrow(() -> new AssertionError("No such method: a"));
    Assert.assertEquals("()I", methodNode.desc);
  }
}
