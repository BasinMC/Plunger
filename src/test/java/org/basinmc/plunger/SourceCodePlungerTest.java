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
package org.basinmc.plunger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.basinmc.plunger.source.SourceCodeTransformer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Evaluates whether {@link SourceCodePlunger} performs as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class SourceCodePlungerTest extends AbstractPlungerTest {

  /**
   * Evaluates whether the source plunger implementation correctly notifies transformers and copies
   * non-transformed files as-is (with the exception of formatting which should match in case of our
   * test file).
   */
  @Test
  public void testApply() throws IOException {
    Path testPath = Paths.get("TestClass.java");
    this.extractSourceFile("/TestClass.java", testPath);

    SourceCodeTransformer transformer = Mockito.mock(SourceCodeTransformer.class);

    SourceCodePlunger plunger = Plunger.sourceBuilder()
        .withTransformer(transformer)
        .build(this.getSource(), this.getTarget());
    plunger.apply();

    Mockito.verify(transformer, Mockito.times(1))
        .transform(Mockito.eq(testPath), Mockito.any());

    Assert.assertTrue(Files.exists(this.getTarget().resolve(testPath)));

    String expected = new String(Files.readAllBytes(this.getSource().resolve(testPath)),
        StandardCharsets.UTF_8);
    String actual = new String(Files.readAllBytes(this.getTarget().resolve(testPath)),
        StandardCharsets.UTF_8);

    Assert.assertEquals(expected, actual); // only works if no transformer touches the class
  }
}
