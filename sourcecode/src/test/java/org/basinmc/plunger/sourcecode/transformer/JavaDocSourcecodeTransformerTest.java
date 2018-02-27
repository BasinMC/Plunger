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
package org.basinmc.plunger.sourcecode.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.basinmc.plunger.sourcecode.SourcecodePlunger;
import org.basinmc.plunger.sourcecode.TestSourcecodeFormatter;
import org.basinmc.plunger.sourcecode.generator.JavaDocGenerator;
import org.basinmc.plunger.test.AbstractPlungerTest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class JavaDocSourcecodeTransformerTest extends AbstractPlungerTest {

  @Test
  public void testExecute() throws IOException {
    Path testFile = Paths.get("TestClass.java");
    this.extractSourceFile("/TestClass.java", testFile);

    JavaDocGenerator generator = Mockito.mock(JavaDocGenerator.class);

    Mockito.when(generator.getClassDocumentation("org/basinmc/plunger/test/TestClass"))
        .thenReturn(Optional.of("Test Documentation"));
    Mockito.when(
        generator.getFieldDocumentation("org/basinmc/plunger/test/TestClass", "testField", "I"))
        .thenReturn(Optional.of("Test Field Documentation"));
    Mockito.when(
        generator.getMethodDocumentation("org/basinmc/plunger/test/TestClass", "<init>",
            "(Ljava/lang/String)V"))
        .thenReturn(Optional.of("Test Constructor Documentation"));
    Mockito.when(
        generator.getMethodDocumentation("org/basinmc/plunger/test/TestClass", "testMethod", "()I"))
        .thenReturn(Optional.of("Test Method Documentation"));

    SourcecodePlunger plunger = SourcecodePlunger.builder()
        .withSourceRelocation(false)
        .withFormatter(new TestSourcecodeFormatter())
        .withTransformer(new JavaDocSourcecodeTransformer(generator))
        .build(this.getSource(), this.getTarget());
    plunger.apply();

    String expected;

    try (InputStream inputStream = JavaDocSourcecodeTransformerTest.class
        .getResourceAsStream("/TestClass.javadoc.java")) {
      byte[] data = new byte[inputStream.available()];
      inputStream.read(data);

      expected = new String(data, StandardCharsets.UTF_8);
    }

    String actual = new String(Files.readAllBytes(this.getTarget().resolve(testFile)),
        StandardCharsets.UTF_8);

    Assert.assertEquals(expected, actual);
  }
}
