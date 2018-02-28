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
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.basinmc.plunger.mapping.MethodNameMapping;

/**
 * Provides a parser for CSV backed method name mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class CSVMethodNameMappingParser extends AbstractCSVMappingParser<MethodNameMapping> {

  private final String classNameColumn;
  private final String originalNameColumn;
  private final String signatureColumn;
  private final String targetNameColumn;

  private CSVMethodNameMappingParser(
      @NonNull CSVFormat format,
      @Nullable String classNameColumn,
      @NonNull String originalNameColumn,
      @NonNull String targetNameColumn,
      @Nullable String signatureColumn) {
    super(format);
    this.classNameColumn = classNameColumn;
    this.originalNameColumn = originalNameColumn;
    this.targetNameColumn = targetNameColumn;
    this.signatureColumn = signatureColumn;
  }

  /**
   * Creates a new empty factory for the method name parser.
   *
   * @return an empty factory.
   */
  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected MethodNameMapping doParse(@NonNull CSVParser parser) throws IOException {
    MethodNameMappingImpl mapping = new MethodNameMappingImpl();

    parser.getRecords().forEach((r) -> {
      String className = null;
      String signature = null;

      if (this.classNameColumn != null) {
        className = r.get(this.classNameColumn);
      }

      if (this.signatureColumn != null) {
        signature = r.get(this.signatureColumn);
      }

      mapping.entries.add(new MethodNameMappingEntry(
          className,
          r.get(this.originalNameColumn),
          r.get(this.targetNameColumn),
          signature
      ));
    });

    return mapping;
  }

  /**
   * Provides a factory for CSV backed method name mapping parsers.
   */
  public static class Builder extends AbstractCSVMappingParser.MemberBuilder {

    private String signatureColumn;

    /**
     * Constructs a new CSV method name mapping parser using the configuration within this builder.
     *
     * @param originalNameColumn the name of the column in which the original method name is
     * stored.
     * @param targetNameColumn the name of the column in which the target method name is stored.
     */
    @NonNull
    public CSVMethodNameMappingParser build(
        @NonNull String originalNameColumn,
        @NonNull String targetNameColumn) {
      return new CSVMethodNameMappingParser(
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
   * Represents a single mapping entry.
   */
  private static final class MethodNameMappingEntry {

    private final String className;
    private final String originalName;
    private final String signature;
    private final String targetName;

    private MethodNameMappingEntry(
        @Nullable String className,
        @NonNull String originalName,
        @NonNull String targetName,
        @Nullable String signature) {
      this.className = className;
      this.originalName = originalName;
      this.targetName = targetName;
      this.signature = signature;
    }

    @NonNull
    public String getTargetName() {
      return this.targetName;
    }

    @NonNull
    public MethodNameMappingEntry invert() {
      return new MethodNameMappingEntry(
          this.className,
          this.targetName,
          this.originalName,
          this.signature
      );
    }

    public boolean matches(@NonNull String className, @NonNull String name,
        @NonNull String signature) {
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
  private static class MethodNameMappingImpl implements MethodNameMapping {

    private final Set<MethodNameMappingEntry> entries = new HashSet<>();

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<String> getMethodName(@NonNull String className, @NonNull String methodName,
        @NonNull String signature) {
      return this.entries.stream()
          .filter((e) -> e.matches(className, methodName, signature))
          .findAny()
          .map(MethodNameMappingEntry::getTargetName);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public MethodNameMapping invert() {
      MethodNameMappingImpl mapping = new MethodNameMappingImpl();
      mapping.entries.addAll(
          this.entries.stream()
              .map(MethodNameMappingEntry::invert)
              .collect(Collectors.toList())
      );
      return mapping;
    }
  }
}
