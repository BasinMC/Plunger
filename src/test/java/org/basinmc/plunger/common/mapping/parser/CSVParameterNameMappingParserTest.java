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
package org.basinmc.plunger.common.mapping.parser;

import java.io.IOException;
import java.io.InputStream;
import org.basinmc.plunger.AbstractPlungerTest;
import org.basinmc.plunger.common.mapping.ParameterNameMapping;
import org.junit.Assert;
import org.junit.Test;

/**
 * Evaluates whether {@link CSVParameterNameMappingParser} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class CSVParameterNameMappingParserTest extends AbstractPlungerTest {

  /**
   * Evaluates whether the parser is capable of correctly parsing standard CSV files and turning
   * them into parameter name mappings.
   */
  @Test
  public void testMapIndexed() throws IOException {
    CSVParameterNameMappingParser parser = CSVParameterNameMappingParser.builder()
        .withClassNameColumn("enclosingClass")
        .withMethodNameColumn("methodName")
        .withSignatureColumn("signature")
        .withParameterIndexColumn("original")
        .build("target");

    try (InputStream inputStream = this.getClass()
        .getResourceAsStream("/csv/ParameterMappingIndexed.csv")) {
      ParameterNameMapping mapping = parser.parse(inputStream);

      String result = mapping
          .getParameterName("org/basinmc/plunger/test/TestClass", "testMethod", "()I",
              "testParameter", 0)
          .orElseThrow(() -> new AssertionError("Expected a mapping but got an empty optional"));
      Assert.assertEquals("a", result);

      Assert.assertFalse(
          mapping.getParameterName("java/lang/Object", "method", "()I", "testParameter", 0)
              .isPresent());
    }
  }

  /**
   * Evaluates whether the parser is capable of correctly parsing standard CSV files and turning
   * them into parameter name mappings.
   */
  @Test
  public void testMapNamed() throws IOException {
    CSVParameterNameMappingParser parser = CSVParameterNameMappingParser.builder()
        .withClassNameColumn("enclosingClass")
        .withMethodNameColumn("methodName")
        .withSignatureColumn("signature")
        .withParameterNameColumn("original")
        .build("target");

    try (InputStream inputStream = this.getClass()
        .getResourceAsStream("/csv/ParameterMappingNamed.csv")) {
      ParameterNameMapping mapping = parser.parse(inputStream);

      String result = mapping
          .getParameterName("org/basinmc/plunger/test/TestClass", "testMethod", "()I",
              "testParameter", 0)
          .orElseThrow(() -> new AssertionError("Expected a mapping but got an empty optional"));
      Assert.assertEquals("a", result);

      Assert.assertFalse(
          mapping.getParameterName("java/lang/Object", "method", "()I", "testParameter", 0)
              .isPresent());
    }
  }
}
