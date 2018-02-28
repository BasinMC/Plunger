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
package org.basinmc.plunger.mapping.parser;

import java.io.IOException;
import java.io.InputStream;
import org.basinmc.plunger.mapping.ParameterNameMapping;
import org.junit.Assert;
import org.junit.Test;

/**
 * Evaluates whether {@link SRGParameterNameMappingParser} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class SRGParameterNameMappingParserTest {

  @Test
  public void testMap() throws IOException {
    SRGParameterNameMappingParser parser = new SRGParameterNameMappingParser();

    try (InputStream inputStream = this.getClass()
        .getResourceAsStream("/TestParameterMapping.properties")) {
      ParameterNameMapping mapping = parser.parse(inputStream);

      String result = mapping
          .getParameterName("org/basinmc/plunger/test/TestClass", "<init>", "(II)V", null, 0)
          .orElseThrow(
              () -> new AssertionError("Expected a parameter mapping but got an empty optional"));
      Assert.assertEquals("parameterA", result);

      result = mapping
          .getParameterName("org/basinmc/plunger/test/TestClass", "<init>", "(II)V", null, 1)
          .orElseThrow(
              () -> new AssertionError("Expected a parameter mapping but got an empty optional"));
      Assert.assertEquals("parameterB", result);

      Assert.assertFalse(
          mapping.getParameterName("org/basinmc/plunger/test/TestClass", "<init>", "(II)V", null, 2)
              .isPresent());
      Assert.assertFalse(
          mapping.getParameterName("java/lang/Object", "testMethod", "(I)V", null, 0).isPresent());
    }
  }
}
