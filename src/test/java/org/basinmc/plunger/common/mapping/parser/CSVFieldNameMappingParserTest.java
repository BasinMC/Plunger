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
import org.apache.commons.csv.CSVFormat;
import org.basinmc.plunger.common.mapping.FieldNameMapping;
import org.junit.Assert;
import org.junit.Test;

/**
 * Evaluates whether {@link CSVFieldNameMappingParser} operates as expected.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class CSVFieldNameMappingParserTest {

  /**
   * Evaluates whether the parser is capable of correctly parsing standard CSV files and turning
   * them into field name mappings.
   */
  @Test
  public void testMap() throws IOException {
    CSVFieldNameMappingParser parser = CSVFieldNameMappingParser.builder()
        .withClassNameColumn("enclosingClass")
        .withSignatureColumn("signature")
        .build("original", "target");

    try (InputStream inputStream = this.getClass().getResourceAsStream("/csv/FieldMapping.csv")) {
      FieldNameMapping mapping = parser.parse(inputStream);

      String result = mapping.getFieldName("org/basinmc/plunger/test/TestClass", "testField", "I")
          .orElseThrow(() -> new AssertionError("Expected a mapping but got an empty optional"));
      Assert.assertEquals("a", result);

      Assert.assertFalse(mapping.getFieldName("java/lang/Object", "field", "I").isPresent());
    }
  }
}
