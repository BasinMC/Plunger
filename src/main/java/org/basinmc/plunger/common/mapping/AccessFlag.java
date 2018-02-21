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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;

/**
 * <p>Represents one or more access preferences which identify whether and how classes are permitted
 * to access each other or their respective members.</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class AccessFlag {

  public static final AccessFlag FINAL = new AccessFlag(0b010000);
  public static final AccessFlag PACKAGE_PRIVATE = new AccessFlag(0b000100);
  public static final AccessFlag PRIVATE = new AccessFlag(0b001000);
  public static final AccessFlag PROTECTED = new AccessFlag(0b000010);
  public static final AccessFlag PUBLIC = new AccessFlag(0b000001);
  /**
   * Defines a mask which permits us to detect and clear duplicate access flags.
   */
  private static final int ACCESS_LEVEL_MASK = 0b001111;

  private final int flag;

  private AccessFlag(int flag) {
    this.flag = flag;
  }

  /**
   * <p>Appends the indicated set of flags to this flag set.</p>
   *
   * <p>If any of the passed flags collide with the local set (such as access levels), they will be
   * automatically unset and replaced with the new property.</p>
   *
   * @param value a selection of flags to add to this set.
   * @return a mutated flag set.
   */
  @NonNull
  public AccessFlag add(@NonNull AccessFlag value) {
    int flag = this.flag;

    // when an access flag is set in the new value, we'll simply clear our local access flag in
    // order to permit overriding of the current flag (e.g. the new definition takes precedence)
    if ((value.flag & ACCESS_LEVEL_MASK) != 0) {
      flag &= ~ACCESS_LEVEL_MASK;
    }

    return new AccessFlag(flag | value.flag);
  }

  /**
   * Evaluates whether this set of flags contains all of the flags set within the passed set.
   *
   * @param value a set of flags.
   * @return true if all are set, false otherwise.
   */
  public boolean contains(@NonNull AccessFlag value) {
    return this == value || (this.flag & value.flag) == value.flag;
  }

  /**
   * Evaluates whether this set of flags contains any of the flags set within the passed set.
   *
   * @param value a set of flags.
   * @return true if at least a single flag is shared, false otherwise.
   */
  public boolean containsAny(@NonNull AccessFlag value) {
    return this == value || (this.flag & value.flag) != 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    AccessFlag that = (AccessFlag) o;
    return this.flag == that.flag;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.flag);
  }

  /**
   * Removes the indicated set of flags from this flag set.
   *
   * @param value a selection of flags to remove from this set.
   * @return a mutated flag set.
   */
  @NonNull
  public AccessFlag remove(@NonNull AccessFlag value) {
    return new AccessFlag(this.flag & (~value.flag));
  }
}
