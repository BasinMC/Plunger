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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.basinmc.plunger.mapping.ClassNameMapping;

/**
 * Provides a parser for CSV backed class name mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class CSVClassNameMappingParser extends AbstractCSVMappingParser<ClassNameMapping> {

  private final String originalNameColumn;
  private final String targetNameColumn;

  private CSVClassNameMappingParser(
      @Nonnull CSVFormat format,
      @Nonnull String originalNameColumn,
      @Nonnull String targetNameColumn) {
    super(format);
    this.originalNameColumn = originalNameColumn;
    this.targetNameColumn = targetNameColumn;
  }

  /**
   * Creates a new empty factory for the class name parser.
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
  protected ClassNameMapping doParse(@Nonnull CSVParser parser) throws IOException {
    ClassNameMappingImpl mapping = new ClassNameMappingImpl();

    parser.getRecords().forEach(
        (r) -> mapping.nameMap.put(r.get(this.originalNameColumn), r.get(this.targetNameColumn)));

    return mapping;
  }

  /**
   * Provides a factory for CSV backed class name mapping parsers.
   */
  public static final class Builder extends AbstractCSVMappingParser.Builder {

    private Builder() {
    }

    /**
     * Constructs a new CSV mapping parser for class name mappings using the configuration of this
     * builder.
     *
     * @param originalNameColumn the name of the column in which the original class name is stored.
     * @param targetNameColumn the name of the column in which the target class name is stored.
     */
    @Nonnull
    public CSVClassNameMappingParser build(
        @Nonnull String originalNameColumn,
        @Nonnull String targetNameColumn) {
      return new CSVClassNameMappingParser(this.format, originalNameColumn, targetNameColumn);
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
  }

  /**
   * Provides a map based class name mapping which is to be filled based on a CSV file.
   */
  private static class ClassNameMappingImpl implements ClassNameMapping {

    private final Map<String, String> nameMap = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<String> getClassName(@NonNull String original) {
      return Optional.ofNullable(this.nameMap.get(original));
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public ClassNameMappingImpl invert() {
      ClassNameMappingImpl mapping = new ClassNameMappingImpl();
      mapping.nameMap.putAll(
          this.nameMap.entrySet().stream()
              .collect(Collectors.toMap(Entry::getValue, Entry::getKey))
      );
      return mapping;
    }
  }
}
