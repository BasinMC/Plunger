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
import org.basinmc.plunger.common.mapping.NameMapping;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class SRGNameMappingParserTest extends AbstractPlungerTest {

  @Test
  public void testMap() throws IOException {
    SRGNameMappingParser parser = new SRGNameMappingParser();

    try (InputStream inputStream = this.getClass().getResourceAsStream("/TestMapping.srg")) {
      NameMapping mapping = parser.parse(inputStream);

      String result = mapping.getClassName("org/basinmc/plunger/test/TestClass")
          .orElseThrow(
              () -> new AssertionError("Expected a class mapping but got an empty optional"));
      Assert.assertEquals("org/basinmc/plunger/mapped/test/MappedTestClass", result);

      result = mapping.getFieldName("org/basinmc/plunger/test/TestClass", "testField", "I")
          .orElseThrow(
              () -> new AssertionError("Expected a field mapping but got an empty optional"));
      Assert.assertEquals("a", result);

      result = mapping.getMethodName("org/basinmc/plunger/test/TestClass", "testMethod", "()I")
          .orElseThrow(
              () -> new AssertionError("Expected a method mapping but got an empty optional"));
      Assert.assertEquals("a", result);

      Assert.assertFalse(mapping.getClassName("java/lang/Object").isPresent());
      Assert.assertFalse(mapping.getFieldName("java/lang/Object", "testField", "I").isPresent());
      Assert
          .assertFalse(mapping.getMethodName("java/lang/Object", "testMethod", "()I").isPresent());
    }
  }
}
