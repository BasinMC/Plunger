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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

/**
 * Provides a base to CSV based mapping parsers which accept arbitrarily formatted files.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class AbstractCSVMappingParser<M> {

  private final CSVFormat format;

  AbstractCSVMappingParser(@Nonnull CSVFormat format) {
    this.format = format;
  }

  /**
   * Parses the contents of a CSV file or stream.
   *
   * @param parser a CSV parser.
   * @return a mapping.
   * @throws IOException when parsing fails.
   */
  protected abstract M doParse(@Nonnull CSVParser parser) throws IOException;

  /**
   * Parses the contents of an arbitrary file.
   *
   * @param file a CSV mapping file.
   * @param charset a charset.
   * @return a set of mappings of a certain type.
   * @throws IOException when parsing the mappings fails.
   */
  @Nonnull
  public M parse(@Nonnull Path file, @Nonnull Charset charset) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
      return this.parse(reader);
    }
  }

  /**
   * @see #parse(InputStream, Charset)
   */
  @Nonnull
  public M parse(@Nonnull InputStream inputStream) throws IOException {
    return this.parse(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Parses the contents of an arbitrary input stream.
   *
   * @param inputStream a CSV stream.
   * @param charset a charset.
   * @return a set of mappings of a certain type.
   * @throws IOException when parsing the mappings fails.
   */
  @Nonnull
  public M parse(@Nonnull InputStream inputStream, @Nonnull Charset charset) throws IOException {
    try (InputStreamReader reader = new InputStreamReader(inputStream, charset)) {
      return this.parse(reader);
    }
  }

  /**
   * Parses the contents of an arbitrary reader.
   *
   * @param reader a reader.
   * @return a set of mappings of a certain type.
   * @throws IOException when parsing the mappings fails.
   */
  @Nonnull
  public M parse(@Nonnull Reader reader) throws IOException {
    CSVParser parser = new CSVParser(reader, this.format);
    return this.doParse(parser);
  }

  /**
   * @see #parse(Path, Charset)
   */
  @Nonnull
  public M parse(@Nonnull Path file) throws IOException {
    return this.parse(file, StandardCharsets.UTF_8);
  }

  /**
   * Provides a base to factories which produce arbitrary CSV mapper parsers.
   */
  public abstract static class Builder {

    protected CSVFormat format = CSVFormat.DEFAULT
        .withFirstRecordAsHeader();

    /**
     * Selects a CSV format to rely upon when parsing file or stream contents.
     *
     * @param format a CSV format.
     * @return a reference to this builder.
     */
    @Nonnull
    public Builder withFormat(@Nonnull CSVFormat format) {
      this.format = format;
      return this;
    }
  }

  /**
   * Provides a base to factories which produce arbitrary CSV mapper parsers for class members
   * elements.
   */
  public abstract static class MemberBuilder extends Builder {

    protected String classNameColumn;

    /**
     * <p>Selects the column in which the enclosing class name will be found.</p>
     *
     * <p>If none is specified (e.g. this method is not called at all or null is passed), the
     * resulting mapper will attempt to match a member based on any other configured factors).</p>
     *
     * @param columnName a column name.
     * @return a reference to this builder.
     */
    @Nonnull
    public MemberBuilder withClassNameColumn(@Nullable String columnName) {
      this.classNameColumn = columnName;
      return this;
    }
  }
}
