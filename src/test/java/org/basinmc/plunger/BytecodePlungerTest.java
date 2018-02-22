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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.basinmc.plunger.bytecode.BytecodeTransformer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Evaluates whether the {@link BytecodePlunger} implementation performs as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class BytecodePlungerTest extends AbstractPlungerTest {

  /**
   * Evaluates whether the bytecode plunger implementation correctly notifies transformers and
   * copies non-transformed files as-is.
   */
  @Test
  public void testTransform() throws IOException {
    Path testPath = Paths.get("TestClass.class");
    this.extractSourceFile("/TestClass.class", testPath);

    BytecodeTransformer transformer = Mockito.mock(BytecodeTransformer.class);
    Mockito.when(transformer
        .createTransformer(Mockito.eq(testPath), Mockito.any()))
        .thenReturn(Optional.empty());

    BytecodePlunger plunger = Plunger.bytecodeBuilder()
        .withTransformer(transformer)
        .build(this.getSource(), this.getTarget());
    plunger.apply();

    Mockito.verify(transformer, Mockito.times(1))
        .createTransformer(Mockito.eq(testPath), Mockito.any());

    Assert.assertTrue(Files.exists(this.getTarget().resolve(testPath)));

    byte[] expected = Files.readAllBytes(this.getSource().resolve(testPath));
    byte[] actual = Files.readAllBytes(this.getTarget().resolve(testPath));

    Assert.assertArrayEquals(expected, actual); // only works if no transformer touches the class
  }
}
