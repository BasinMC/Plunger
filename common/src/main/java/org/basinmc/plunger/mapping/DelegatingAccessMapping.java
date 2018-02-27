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
package org.basinmc.plunger.mapping;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * <p>Provides a delegation based access mapping which queries multiple mapping implementations in
 * order to produce a target access level.</p>
 *
 * <p>The configured mappings will be invoked in their respective order of registration using the
 * access level of the previous mapping (if any; original value otherwise). As such, the last
 * mapping within the list will gain the highest precedence (e.g. may override the access level
 * completely if desired).</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class DelegatingAccessMapping implements AccessMapping {

  private final List<ClassAccessMapping> classAccessMappings;
  private final List<FieldAccessMapping> fieldAccessMappings;
  private final List<MethodAccessMapping> methodAccessMappings;

  protected DelegatingAccessMapping(
      List<ClassAccessMapping> classAccessMappings,
      List<FieldAccessMapping> fieldAccessMappings,
      List<MethodAccessMapping> methodAccessMappings) {
    this.classAccessMappings = new ArrayList<>(classAccessMappings);
    this.fieldAccessMappings = new ArrayList<>(fieldAccessMappings);
    this.methodAccessMappings = new ArrayList<>(methodAccessMappings);
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<AccessFlag> getClassAccessFlags(@NonNull String className,
      @NonNull AccessFlag flags) {
    AccessFlag result = flags;

    for (ClassAccessMapping mapping : this.classAccessMappings) {
      result = mapping.getClassAccessFlags(className, result)
          .orElse(result);
    }

    return Optional.of(result)
        .filter((f) -> !flags.equals(f));
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<AccessFlag> getFieldAccessFlags(@NonNull String className,
      @NonNull String fieldName, @NonNull String signature, @NonNull AccessFlag flags) {
    AccessFlag result = flags;

    for (FieldAccessMapping mapping : this.fieldAccessMappings) {
      result = mapping.getFieldAccessFlags(className, fieldName, signature, result)
          .orElse(result);
    }

    return Optional.of(result)
        .filter((f) -> !flags.equals(f));
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<AccessFlag> getMethodAccessFlags(@NonNull String className,
      @NonNull String methodName, @NonNull String signature, @NonNull AccessFlag flags) {
    AccessFlag result = flags;

    for (MethodAccessMapping mapping : this.methodAccessMappings) {
      result = mapping.getMethodAccessFlags(className, methodName, signature, result)
          .orElse(result);
    }

    return Optional.of(result)
        .filter((f) -> !flags.equals(f));
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public AccessMapping invert() {
    return new DelegatingAccessMapping(
        this.classAccessMappings.stream().map(ClassAccessMapping::invert)
            .collect(Collectors.toList()),
        this.fieldAccessMappings.stream().map(FieldAccessMapping::invert)
            .collect(Collectors.toList()),
        this.methodAccessMappings.stream().map(MethodAccessMapping::invert)
            .collect(Collectors.toList())
    );
  }


  /**
   * Provides a factory for delegating mapping instances.
   */
  public static final class Builder {

    private final List<ClassAccessMapping> classAccessMappings = new ArrayList<>();
    private final List<FieldAccessMapping> fieldAccessMappings = new ArrayList<>();
    private final List<MethodAccessMapping> methodAccessMappings = new ArrayList<>();

    private Builder() {
    }

    /**
     * Constructs a new delegating mapping based on the current configuration of this builder.
     *
     * @return a delegating mapping.
     */
    @Nonnull
    public DelegatingAccessMapping build() {
      return new DelegatingAccessMapping(
          this.classAccessMappings,
          this.fieldAccessMappings,
          this.methodAccessMappings
      );
    }

    /**
     * Appends a class access mapping to the delegation.
     *
     * @param mapping a class access mapping.
     * @return a reference to this builder.
     */
    @Nonnull
    public Builder withClassMapping(@Nonnull ClassAccessMapping mapping) {
      this.classAccessMappings.add(mapping);
      return this;
    }

    /**
     * Appends a field access mapping to the delegation.
     *
     * @param mapping a field access mapping.
     * @return a reference to this builder.
     */
    @Nonnull
    public Builder withFieldAccessMapping(@Nonnull FieldAccessMapping mapping) {
      this.fieldAccessMappings.add(mapping);
      return this;
    }

    /**
     * Appends a access mapping to the delegation.
     *
     * @param mapping a full access mapping.
     * @return a reference to this builder.
     */
    @Nonnull
    public Builder withMapping(@Nonnull AccessMapping mapping) {
      this.classAccessMappings.add(mapping);
      this.fieldAccessMappings.add(mapping);
      this.methodAccessMappings.add(mapping);
      return this;
    }

    /**
     * Appends a method access mapping to the delegation.
     *
     * @param mapping a method access mapping.
     * @return a reference to this builder.
     */
    @Nonnull
    public Builder withMethodAccessMapping(@Nonnull MethodAccessMapping mapping) {
      this.methodAccessMappings.add(mapping);
      return this;
    }
  }
}
