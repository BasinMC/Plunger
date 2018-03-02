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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>Provides a name mapping implementation which delegates its lookups to an arbitrary set of
 * mappings.</p>
 *
 * <p>For the purposes of this implementation, mappings will be invoked in a particular order and
 * considered chainable (e.g. following mappings will be passed the result of the previous call in
 * order to permit chaining of different definitions).</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class DelegatingNameMapping implements NameMapping {

  private final List<ClassNameMapping> classNameMappings;
  private final List<FieldNameMapping> fieldNameMappings;
  private final List<MethodNameMapping> methodNameMappings;
  private final List<ParameterNameMapping> parameterNameMappings;
  private final boolean resolveEnclosure;

  protected DelegatingNameMapping(
      @NonNull List<ClassNameMapping> classNameMappings,
      @NonNull List<FieldNameMapping> fieldNameMappings,
      @NonNull List<MethodNameMapping> methodNameMappings,
      @NonNull List<ParameterNameMapping> parameterNameMappings,
      boolean resolveEnclosure) {
    this.classNameMappings = new ArrayList<>(classNameMappings);
    this.fieldNameMappings = new ArrayList<>(fieldNameMappings);
    this.methodNameMappings = new ArrayList<>(methodNameMappings);
    this.parameterNameMappings = new ArrayList<>(parameterNameMappings);
    this.resolveEnclosure = resolveEnclosure;
  }

  /**
   * Constructs a new factory for delegating name mapper instances.
   *
   * @return a new builder instance.
   */
  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DelegatingNameMapping)) {
      return false;
    }
    DelegatingNameMapping that = (DelegatingNameMapping) o;
    return Objects.equals(this.classNameMappings, that.classNameMappings) &&
        Objects.equals(this.fieldNameMappings, that.fieldNameMappings) &&
        Objects.equals(this.methodNameMappings, that.methodNameMappings);
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<String> getClassName(@NonNull String original) {
    String result = original;

    for (ClassNameMapping mapping : this.classNameMappings) {
      result = mapping.getClassName(result)
          .orElse(result);
    }

    return Optional.of(result)
        .filter((r) -> !original.equals(r));
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<String> getFieldName(@NonNull String className, @NonNull String fieldName,
      @NonNull String signature) {
    String result = fieldName;

    if (this.resolveEnclosure) {
      className = this.getClassName(className)
          .orElse(className);
    }

    for (FieldNameMapping mapping : this.fieldNameMappings) {
      result = mapping.getFieldName(className, result, signature)
          .orElse(result);
    }

    return Optional.of(result)
        .filter((r) -> !fieldName.equals(r));

  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<String> getMethodName(@NonNull String className, @NonNull String methodName,
      @NonNull String signature) {
    String result = methodName;

    if (this.resolveEnclosure) {
      className = this.getClassName(className)
          .orElse(className);
    }

    for (MethodNameMapping mapping : this.methodNameMappings) {
      result = mapping.getMethodName(className, result, signature)
          .orElse(result);
    }

    return Optional.of(result)
        .filter((r) -> !methodName.equals(r));
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public Optional<String> getParameterName(@NonNull String className, @NonNull String methodName,
      @NonNull String signature, @NonNull String parameterName, int parameterIndex) {
    String result = parameterName;

    if (this.resolveEnclosure) {
      methodName = this.getMethodName(className, methodName, signature)
          .orElse(methodName);

      className = this.getClassName(className)
          .orElse(className);
    }

    for (ParameterNameMapping mapping : this.parameterNameMappings) {
      result = mapping.getParameterName(className, methodName, signature, result, parameterIndex)
          .orElse(result);
    }

    return Optional.of(result)
        .filter((r) -> !Objects.equals(parameterName, r));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.classNameMappings, this.fieldNameMappings, this.methodNameMappings);
  }

  /**
   * {@inheritDoc}
   */
  @NonNull
  @Override
  public DelegatingNameMapping invert() {
    return new DelegatingNameMapping(
        this.classNameMappings.stream().map(ClassNameMapping::invert).collect(Collectors.toList()),
        this.fieldNameMappings.stream().map(FieldNameMapping::invert).collect(Collectors.toList()),
        this.methodNameMappings.stream().map(MethodNameMapping::invert)
            .collect(Collectors.toList()),
        this.parameterNameMappings.stream().map(ParameterNameMapping::invert)
            .collect(Collectors.toList()),
        this.resolveEnclosure
    );
  }

  /**
   * Provides a factory for delegating mapping instances.
   */
  public static final class Builder {

    private final List<ClassNameMapping> classNameMappings = new ArrayList<>();
    private final List<FieldNameMapping> fieldNameMappings = new ArrayList<>();
    private final List<MethodNameMapping> methodNameMappings = new ArrayList<>();
    private final List<ParameterNameMapping> parameterNameMappings = new ArrayList<>();
    private boolean resolveEnclosure;

    private Builder() {
    }

    /**
     * Constructs a new delegating mapping based on the current configuration of this builder.
     *
     * @return a delegating mapping.
     */
    @NonNull
    public DelegatingNameMapping build() {
      return new DelegatingNameMapping(this.classNameMappings, this.fieldNameMappings,
          this.methodNameMappings, this.parameterNameMappings, this.resolveEnclosure);
    }

    /**
     * Appends a class name mapping to the delegation.
     *
     * @param mapping a class name mapping.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withClassMapping(@NonNull ClassNameMapping mapping) {
      this.classNameMappings.add(mapping);
      return this;
    }

    /**
     * Appends a field name mapping to the delegation.
     *
     * @param mapping a field name mapping.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withFieldNameMapping(@NonNull FieldNameMapping mapping) {
      this.fieldNameMappings.add(mapping);
      return this;
    }

    /**
     * Appends a name mapping to the delegation.
     *
     * @param mapping a full name mapping.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withMapping(@NonNull NameMapping mapping) {
      this.classNameMappings.add(mapping);
      this.fieldNameMappings.add(mapping);
      this.methodNameMappings.add(mapping);
      this.parameterNameMappings.add(mapping);
      return this;
    }

    /**
     * Appends a method name mapping to the delegation.
     *
     * @param mapping a method name mapping.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withMethodNameMapping(@NonNull MethodNameMapping mapping) {
      this.methodNameMappings.add(mapping);
      return this;
    }

    /**
     * Appends a parameter name mapping to the delegation.
     *
     * @param mapping a parameter name mapping.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withParameterNameMapping(@NonNull ParameterNameMapping mapping) {
      this.parameterNameMappings.add(mapping);
      return this;
    }

    /**
     * @see #withResolveEnclosure(boolean)
     */
    @NonNull
    public Builder withResolveEnclosure() {
      return this.withResolveEnclosure(true);
    }

    /**
     * Selects whether or not enclosed mapped elements (such as fields or methods) shall be passed a
     * resolved owner type (since mappings typically pass the original name as the owner value).
     *
     * @param value true if enabled, false otherwise
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withResolveEnclosure(boolean value) {
      this.resolveEnclosure = value;
      return this;
    }
  }
}
