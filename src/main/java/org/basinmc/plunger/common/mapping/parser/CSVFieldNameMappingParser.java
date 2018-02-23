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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.basinmc.plunger.common.mapping.FieldNameMapping;

/**
 * Provides a parser for CSV backed field name mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class CSVFieldNameMappingParser extends AbstractCSVMappingParser<FieldNameMapping> {

  private final String classNameColumn;
  private final String originalNameColumn;
  private final String signatureColumn;
  private final String targetNameColumn;

  private CSVFieldNameMappingParser(
      @Nonnull CSVFormat format,
      @Nullable String classNameColumn,
      @Nonnull String originalNameColumn,
      @Nonnull String targetNameColumn,
      @Nullable String signatureColumn) {
    super(format);
    this.classNameColumn = classNameColumn;
    this.originalNameColumn = originalNameColumn;
    this.targetNameColumn = targetNameColumn;
    this.signatureColumn = signatureColumn;
  }

  /**
   * Creates a new empty factory for the field name parser.
   *
   * @return an empty factory.
   */
  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected FieldNameMapping doParse(@Nonnull CSVParser parser) throws IOException {
    FieldNameMappingImpl mapping = new FieldNameMappingImpl();

    parser.getRecords().forEach((r) -> {
      String className = null;
      String signature = null;

      if (this.classNameColumn != null) {
        className = r.get(this.classNameColumn);
      }

      if (this.signatureColumn != null) {
        signature = r.get(this.signatureColumn);
      }

      mapping.entries.add(new FieldNameMappingEntry(
          className,
          r.get(this.originalNameColumn),
          r.get(this.targetNameColumn),
          signature
      ));
    });

    return mapping;
  }

  /**
   * Provides a factory for CSV backed field name mapping parsers.
   */
  public static class Builder extends AbstractCSVMappingParser.MemberBuilder {

    private String signatureColumn;

    /**
     * Constructs a new CSV field name mapping parser using the configuration within this builder.
     *
     * @param originalNameColumn the name of the column in which the original field name is stored.
     * @param targetNameColumn the name of the column in which the target field name is stored.
     */
    @Nonnull
    public CSVFieldNameMappingParser build(
        @Nonnull String originalNameColumn,
        @Nonnull String targetNameColumn) {
      return new CSVFieldNameMappingParser(
          this.format,
          this.classNameColumn,
          originalNameColumn,
          targetNameColumn,
          this.signatureColumn
      );
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Builder withClassNameColumn(@Nullable String columnName) {
      super.withClassNameColumn(columnName);
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Builder withFormat(@Nonnull CSVFormat format) {
      super.withFormat(format);
      return this;
    }

    /**
     * <p>Selects the column in which the ASM signature for the field will be found.</p>
     *
     * <p>If none is specified (e.g. this method is not called at all or null is passed), the
     * resulting mapper will only evaluate whether or not the field name and class name match
     * (assuming that a class name index was configured).</p>
     *
     * @param columnName a column name.
     * @return a reference to this builder.
     */
    @Nonnull
    public Builder withSignatureColumn(@Nullable String columnName) {
      this.signatureColumn = columnName;
      return this;
    }
  }

  /**
   * Represents a single mapping entry.
   */
  private static final class FieldNameMappingEntry {

    private final String className;
    private final String originalName;
    private final String signature;
    private final String targetName;

    private FieldNameMappingEntry(
        @Nullable String className,
        @Nonnull String originalName,
        @Nonnull String targetName,
        @Nullable String signature) {
      this.className = className;
      this.originalName = originalName;
      this.targetName = targetName;
      this.signature = signature;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof FieldNameMappingEntry)) {
        return false;
      }
      FieldNameMappingEntry that = (FieldNameMappingEntry) o;
      return Objects.equals(this.className, that.className) &&
          Objects.equals(this.originalName, that.originalName) &&
          Objects.equals(this.targetName, that.targetName) &&
          Objects.equals(this.signature, that.signature);
    }

    @Nonnull
    public String getTargetName() {
      return this.targetName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(this.className, this.originalName, this.targetName, this.signature);
    }

    @Nonnull
    public FieldNameMappingEntry invert() {
      return new FieldNameMappingEntry(
          this.className,
          this.targetName,
          this.originalName,
          this.signature
      );
    }

    public boolean matches(@Nonnull String className, @Nonnull String name,
        @Nonnull String signature) {
      if (this.className != null && !this.className.equals(className)) {
        return false;
      }

      if (this.signature != null && !this.signature.equals(signature)) {
        return false;
      }

      return this.originalName.equals(name);
    }
  }

  /**
   * Provides a parsed representation of CSV backed mappings.
   */
  private static final class FieldNameMappingImpl implements FieldNameMapping {

    private final Set<FieldNameMappingEntry> entries = new HashSet<>();

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<String> getFieldName(@NonNull String className, @NonNull String fieldName,
        @NonNull String signature) {
      return this.entries.stream()
          .filter((e) -> e.matches(className, fieldName, signature))
          .findAny()
          .map(FieldNameMappingEntry::getTargetName);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public FieldNameMappingImpl invert() {
      FieldNameMappingImpl mapping = new FieldNameMappingImpl();
      mapping.entries.addAll(
          this.entries.stream()
              .map(FieldNameMappingEntry::invert)
              .collect(Collectors.toList())
      );
      return mapping;
    }
  }
}
