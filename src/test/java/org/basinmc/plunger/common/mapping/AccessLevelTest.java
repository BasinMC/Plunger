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
package org.basinmc.plunger.common.mapping;

import org.junit.Assert;
import org.junit.Test;

/**
 * Evaluates whether the {@link AccessFlag} implementation acts within its expected bounds.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class AccessLevelTest {

  /**
   * Evaluates whether the add mutation method properly identifies colliding flags and replaces
   * them.
   */
  @Test
  public void testAdd() {
    AccessFlag flag = AccessFlag.PUBLIC;

    // test adding a new standard flag such as the final flag
    Assert.assertFalse(flag.contains(AccessFlag.FINAL));
    flag = flag.add(AccessFlag.FINAL);
    Assert.assertTrue(flag.contains(AccessFlag.FINAL));
    flag = flag.add(AccessFlag.FINAL);
    Assert.assertTrue(flag.contains(AccessFlag.FINAL));

    // test whether access levels override each other
    Assert.assertTrue(flag.contains(AccessFlag.PUBLIC));
    Assert.assertFalse(flag.contains(AccessFlag.PROTECTED));
    flag = flag.add(AccessFlag.PROTECTED);
    Assert.assertFalse(flag.contains(AccessFlag.PUBLIC));
    Assert.assertTrue(flag.contains(AccessFlag.PROTECTED));
    flag = flag.add(AccessFlag.PACKAGE_PRIVATE);
    Assert.assertFalse(flag.contains(AccessFlag.PROTECTED));
    Assert.assertTrue(flag.contains(AccessFlag.PACKAGE_PRIVATE));
    flag = flag.add(AccessFlag.PRIVATE);
    Assert.assertFalse(flag.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertTrue(flag.contains(AccessFlag.PRIVATE));
  }

  /**
   * Evaluates whether flags correctly identify the list of flags which they contain when queried.
   */
  @Test
  public void testContains() {
    AccessFlag flag1 = AccessFlag.PUBLIC;
    AccessFlag flag2 = AccessFlag.PROTECTED;
    AccessFlag flag3 = AccessFlag.PACKAGE_PRIVATE;
    AccessFlag flag4 = AccessFlag.PRIVATE;
    AccessFlag flag5 = AccessFlag.FINAL;

    AccessFlag flag6 = AccessFlag.FINAL.add(AccessFlag.PUBLIC);
    AccessFlag flag7 = AccessFlag.FINAL.add(AccessFlag.PROTECTED);
    AccessFlag flag8 = AccessFlag.FINAL.add(AccessFlag.PACKAGE_PRIVATE);
    AccessFlag flag9 = AccessFlag.FINAL.add(AccessFlag.PRIVATE);

    Assert.assertTrue(flag1.contains(flag1));
    Assert.assertTrue(flag1.contains(AccessFlag.PUBLIC));
    Assert.assertFalse(flag1.contains(AccessFlag.PROTECTED));
    Assert.assertFalse(flag1.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertFalse(flag1.contains(AccessFlag.PRIVATE));
    Assert.assertFalse(flag1.contains(AccessFlag.FINAL));

    Assert.assertTrue(flag2.contains(flag2));
    Assert.assertFalse(flag2.contains(AccessFlag.PUBLIC));
    Assert.assertTrue(flag2.contains(AccessFlag.PROTECTED));
    Assert.assertFalse(flag2.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertFalse(flag2.contains(AccessFlag.PRIVATE));
    Assert.assertFalse(flag2.contains(AccessFlag.FINAL));

    Assert.assertTrue(flag3.contains(flag3));
    Assert.assertFalse(flag3.contains(AccessFlag.PUBLIC));
    Assert.assertFalse(flag3.contains(AccessFlag.PROTECTED));
    Assert.assertTrue(flag3.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertFalse(flag3.contains(AccessFlag.PRIVATE));
    Assert.assertFalse(flag3.contains(AccessFlag.FINAL));

    Assert.assertTrue(flag4.contains(flag4));
    Assert.assertFalse(flag4.contains(AccessFlag.PUBLIC));
    Assert.assertFalse(flag4.contains(AccessFlag.PROTECTED));
    Assert.assertFalse(flag4.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertTrue(flag4.contains(AccessFlag.PRIVATE));
    Assert.assertFalse(flag4.contains(AccessFlag.FINAL));

    Assert.assertTrue(flag5.contains(flag5));
    Assert.assertFalse(flag5.contains(AccessFlag.PUBLIC));
    Assert.assertFalse(flag5.contains(AccessFlag.PROTECTED));
    Assert.assertFalse(flag5.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertFalse(flag5.contains(AccessFlag.PRIVATE));
    Assert.assertTrue(flag5.contains(AccessFlag.FINAL));

    Assert.assertTrue(flag6.contains(flag6));
    Assert.assertTrue(flag6.contains(AccessFlag.PUBLIC));
    Assert.assertFalse(flag6.contains(AccessFlag.PROTECTED));
    Assert.assertFalse(flag6.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertFalse(flag6.contains(AccessFlag.PRIVATE));
    Assert.assertTrue(flag6.contains(AccessFlag.FINAL));

    Assert.assertTrue(flag7.contains(flag7));
    Assert.assertFalse(flag7.contains(AccessFlag.PUBLIC));
    Assert.assertTrue(flag7.contains(AccessFlag.PROTECTED));
    Assert.assertFalse(flag7.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertFalse(flag7.contains(AccessFlag.PRIVATE));
    Assert.assertTrue(flag7.contains(AccessFlag.FINAL));

    Assert.assertTrue(flag8.contains(flag8));
    Assert.assertFalse(flag8.contains(AccessFlag.PUBLIC));
    Assert.assertFalse(flag8.contains(AccessFlag.PROTECTED));
    Assert.assertTrue(flag8.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertFalse(flag8.contains(AccessFlag.PRIVATE));
    Assert.assertTrue(flag8.contains(AccessFlag.FINAL));

    Assert.assertTrue(flag9.contains(flag9));
    Assert.assertFalse(flag9.contains(AccessFlag.PUBLIC));
    Assert.assertFalse(flag9.contains(AccessFlag.PROTECTED));
    Assert.assertFalse(flag9.contains(AccessFlag.PACKAGE_PRIVATE));
    Assert.assertTrue(flag9.contains(AccessFlag.PRIVATE));
    Assert.assertTrue(flag9.contains(AccessFlag.FINAL));
  }

  /**
   * Evaluates whether the remove mutation method properly removes the specified flags.
   */
  @Test
  public void testRemove() {
    AccessFlag flag = AccessFlag.PUBLIC;
    Assert.assertTrue(flag.contains(AccessFlag.PUBLIC));
    flag = flag.remove(AccessFlag.PUBLIC);
    Assert.assertFalse(flag.contains(AccessFlag.PUBLIC));

    flag = AccessFlag.PUBLIC.add(AccessFlag.FINAL);
    Assert.assertTrue(flag.contains(AccessFlag.PUBLIC));
    Assert.assertTrue(flag.contains(AccessFlag.FINAL));
    flag = flag.remove(AccessFlag.FINAL);
    Assert.assertTrue(flag.contains(AccessFlag.PUBLIC));
    Assert.assertFalse(flag.contains(AccessFlag.FINAL));
  }
}
