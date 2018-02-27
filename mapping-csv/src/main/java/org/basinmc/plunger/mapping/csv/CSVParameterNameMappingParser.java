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
package org.basinmc.plunger.mapping.csv;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.basinmc.plunger.mapping.ParameterNameMapping;

/**
 * Provides a parser for CSV based parameter name mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class CSVParameterNameMappingParser extends
    AbstractCSVMappingParser<ParameterNameMapping> {

  private final String classNameColumn;
  private final String methodNameColumn;
  private final String parameterIndexColumn;
  private final String parameterNameColumn;
  private final String signatureColumn;
  private final String targetNameColumn;

  CSVParameterNameMappingParser(
      @NonNull CSVFormat format,
      @Nullable String classNameColumn,
      @Nullable String methodNameColumn,
      @Nullable String signatureColumn,
      @Nullable String parameterNameColumn,
      @Nullable String parameterIndexColumn,
      @NonNull String targetNameColumn) {
    super(format);

    if (parameterIndexColumn == null && parameterNameColumn == null) {
      throw new IllegalArgumentException(
          "Illegal configuration: Either parameter name or parameter index column is required");
    }

    this.classNameColumn = classNameColumn;
    this.methodNameColumn = methodNameColumn;
    this.signatureColumn = signatureColumn;
    this.parameterNameColumn = parameterNameColumn;
    this.parameterIndexColumn = parameterIndexColumn;
    this.targetNameColumn = targetNameColumn;
  }

  /**
   * Constructs a new empty factory for parameter name mapping instances.
   *
   * @return a factory.
   */
  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ParameterNameMapping doParse(@NonNull CSVParser parser) throws IOException {
    ParameterNameMappingImpl mapping = new ParameterNameMappingImpl(
        this.parameterNameColumn != null);
    mapping.entries.addAll(
        parser.getRecords().stream()
            .map((r) -> new ParameterNameMappingEntry(
                this.classNameColumn != null ? r.get(this.classNameColumn) : null,
                this.methodNameColumn != null ? r.get(this.methodNameColumn) : null,
                this.signatureColumn != null ? r.get(this.signatureColumn) : null,
                this.parameterNameColumn != null ? r.get(this.parameterNameColumn) : null,
                this.parameterIndexColumn != null ? Integer
                    .parseUnsignedInt(r.get(this.parameterIndexColumn)) : null,
                r.get(this.targetNameColumn)
            ))
            .collect(Collectors.toSet())
    );
    return mapping;
  }

  /**
   * Provides a factory for parameter name mapping parser instances.
   */
  public static final class Builder extends AbstractCSVMappingParser.MemberBuilder {

    private String methodNameColumn;
    private String parameterIndexColumn;
    private String parameterNameColumn;
    private String signatureColumn;

    private Builder() {
    }

    /**
     * Constructs a new CSV parameter name mapping parser based on the current builder
     * configuration.
     *
     * @param targetNameColumn a target name column index.
     * @return a name mapping parser.
     * @throws IllegalStateException when neither parameterNameColumn or parameterIndexColumn are
     * defined.
     */
    @NonNull
    public CSVParameterNameMappingParser build(@NonNull String targetNameColumn) {
      if (this.parameterNameColumn == null && this.parameterIndexColumn == null) {
        throw new IllegalStateException(
            "Illegal configuration: Either of parameterNameColumn or parameterIndexColumn needs to be defined");
      }

      return new CSVParameterNameMappingParser(
          this.format,
          this.classNameColumn,
          this.methodNameColumn,
          this.signatureColumn,
          this.parameterNameColumn,
          this.parameterIndexColumn,
          targetNameColumn
      );
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withClassNameColumn(@Nullable String columnName) {
      super.withClassNameColumn(columnName);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Builder withFormat(@NonNull CSVFormat format) {
      super.withFormat(format);
      return this;
    }

    /**
     * <p>Selects the column in which the method name will be found.</p>
     *
     * <p>If none is specified (e.g. this method is not called at all or null is passed), the
     * resulting mapper will only evaluate whether or not the method name, class name and parameter
     * name/index match (assuming that a class name and signature name index was configured).</p>
     *
     * @param columnName a column index.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withMethodNameColumn(@Nullable String columnName) {
      this.methodNameColumn = columnName;
      return this;
    }

    /**
     * Selects the column in which the original parameter index will be found.
     *
     * @param columnName a column index.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withParameterIndexColumn(@Nullable String columnName) {
      this.parameterIndexColumn = columnName;
      return this;
    }

    /**
     * Selects the column in which the original parameter name will be found.
     *
     * @param columnName a column index.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withParameterNameColumn(@Nullable String columnName) {
      this.parameterNameColumn = columnName;
      return this;
    }

    /**
     * <p>Selects the column in which the ASM signature for the method will be found.</p>
     *
     * <p>If none is specified (e.g. this method is not called at all or null is passed), the
     * resulting mapper will only evaluate whether or not the method name and class name match
     * (assuming that a class name index was configured).</p>
     *
     * @param columnName a column index.
     * @return a reference to this builder.
     */
    @NonNull
    public Builder withSignatureColumn(@Nullable String columnName) {
      this.signatureColumn = columnName;
      return this;
    }
  }

  /**
   * Represents a single parsed parameter name entry.
   */
  private static final class ParameterNameMappingEntry {

    private final String className;
    private final String methodName;
    private final Integer parameterIndex;
    private final String parameterName;
    private final String signature;
    private final String targetName;

    private ParameterNameMappingEntry(
        @Nullable String className,
        @Nullable String methodName,
        @Nullable String signature,
        @Nullable String parameterName,
        @Nullable Integer parameterIndex,
        @NonNull String targetName) {
      this.className = className;
      this.methodName = methodName;
      this.signature = signature;
      this.parameterName = parameterName;
      this.parameterIndex = parameterIndex;
      this.targetName = targetName;

      assert this.parameterName != null || this.parameterIndex != null;
    }

    @NonNull
    public String getTargetName() {
      return this.targetName;
    }

    public boolean matches(@NonNull String className, @NonNull String methodName,
        @NonNull String signature, @NonNull String parameterName, int parameterIndex) {
      if (this.className != null && !this.className.equals(className)) {
        return false;
      }

      if (this.methodName != null && !this.methodName.equals(methodName)) {
        return false;
      }

      if (this.signature != null && !this.signature.equals(signature)) {
        return false;
      }

      if (this.parameterName != null) {
        return this.parameterName.equals(parameterName);
      }

      assert this.parameterIndex != null;
      return this.parameterIndex == parameterIndex;
    }
  }

  /**
   * Provides a parsed representation of a parameter name mapping.
   */
  private static final class ParameterNameMappingImpl implements ParameterNameMapping {

    private final Collection<ParameterNameMappingEntry> entries = new HashSet<>();
    private final boolean nameAvailable;

    private ParameterNameMappingImpl(boolean nameAvailable) {
      this.nameAvailable = nameAvailable;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<String> getParameterName(@NonNull String className, @NonNull String methodName,
        @NonNull String signature, @Nullable String parameterName, int parameterIndex) {
      return this.entries.stream()
          .filter((e) -> e.matches(className, methodName, signature, parameterName, parameterIndex))
          .findAny()
          .map(ParameterNameMappingEntry::getTargetName);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public ParameterNameMapping invert() {
      if (!this.nameAvailable) {
        throw new UnsupportedOperationException();
      }

      ParameterNameMappingImpl mapping = new ParameterNameMappingImpl(true);
      mapping.entries.addAll(
          mapping.entries.stream()
              .map((e) -> {
                assert e.parameterName != null;
                return new ParameterNameMappingEntry(e.className, e.methodName, e.signature,
                    e.targetName, null, e.parameterName);
              })
              .collect(Collectors.toSet())
      );

      return mapping;
    }
  }
}
