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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.basinmc.plunger.common.mapping.NameMapping;

/**
 * Provides a parser for SRG based mappings.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class SRGNameMappingParser {

  /**
   * @see #parse(Path, Charset)
   */
  @Nonnull
  public NameMapping parse(@Nonnull Path path) throws IOException {
    return this.parse(path, StandardCharsets.UTF_8);
  }

  /**
   * Parses the SRG mappings provided by an arbitrary file.
   *
   * @param path an input file.
   * @return a parsed mapping.
   * @throws IOException when accessing the file fails.
   */
  @Nonnull
  public NameMapping parse(@Nonnull Path path, @Nonnull Charset charset) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
      return this.parse(reader);
    }
  }

  /**
   * @see #parse(InputStream, Charset)
   */
  @Nonnull
  public NameMapping parse(@Nonnull InputStream inputStream) throws IOException {
    return this.parse(inputStream, StandardCharsets.UTF_8);
  }

  /**
   * Parses the SRG mappings provided by an arbitrary input stream.
   *
   * @param inputStream an input stream.
   * @param charset a charset.
   * @return a parsed mapping.
   * @throws IOException when accessing the input stream fails.
   */
  @Nonnull
  public NameMapping parse(@Nonnull InputStream inputStream, @Nonnull Charset charset)
      throws IOException {
    try (InputStreamReader reader = new InputStreamReader(inputStream, charset)) {
      return this.parse(reader);
    }
  }

  /**
   * Parses the SRG mappings provided by an arbitrary reader.
   *
   * @param reader a reader.
   * @return a parsed mapping.
   * @throws IOException when accessing the reader fails.
   */
  @Nonnull
  public NameMapping parse(@Nonnull Reader reader) throws IOException {
    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
      return this.parse(bufferedReader);
    }
  }

  /**
   * Parses the SRG mappings provided by an arbitrary reader.
   *
   * @param reader a buffered reader.
   * @return a parsed mapping.
   */
  @Nonnull
  public NameMapping parse(@Nonnull BufferedReader reader) throws IOException {
    NameMappingImpl mapping = new NameMappingImpl();

    int lineNumber = 1;
    Iterator<String> it = reader.lines().iterator();

    while (it.hasNext()) {
      String line = it.next();

      // skip any empty lines as we do not really care about them
      if (line.trim().isEmpty()) {
        continue;
      }

      String type = line.substring(0, 2);
      line = line.substring(4);

      switch (type) {
        // we are not interested in package level mappings as they're handled automatically
        case "PK":
          break;
        case "CL":
          this.parseClassMapping(mapping, lineNumber, line);
          break;
        case "FD":
          this.parseFieldMapping(mapping, lineNumber, line);
          break;
        case "MD":
          this.parseMethodMapping(mapping, lineNumber, line);
          break;
        default:
          throw new IOException(
              "Illegal instruction \"" + type + "\": Expected any of PK, CL, FD or MD");
      }

      ++lineNumber;
    }

    return mapping;
  }

  /**
   * Parses a class mapping SRG line.
   *
   * @param mapping a mapping instance.
   * @param lineNumber the current line number.
   * @param line an unparsed SRG line.
   * @throws IOException when parsing fails.
   */
  private void parseClassMapping(@Nonnull NameMappingImpl mapping, int lineNumber,
      @Nonnull String line) throws IOException {
    String[] elements = line.split(" ");

    if (elements.length != 2) {
      throw new IOException("Illegal class mapping on line " + lineNumber
          + ": Expected exactly two parameters - Found " + elements.length);
    }

    mapping.classNameMap.put(elements[0], elements[1]);
  }

  /**
   * Parses a field mapping SRG line.
   *
   * @param mapping a mapping instance.
   * @param lineNumber the current line number.
   * @param line an unparsed SRG line.
   * @throws IOException when parsing fails.
   */
  private void parseFieldMapping(@Nonnull NameMappingImpl mapping, int lineNumber,
      @Nonnull String line) throws IOException {
    String[] elements = line.split(" ");

    if (elements.length != 2) {
      throw new IOException("Illegal field mapping on line " + lineNumber
          + ": Expected exactly two parameters - Found " + elements.length);
    }

    // SRG essentially gives us two parameters here. The old fully qualified name and the new one
    // (where the field is separated from its enclosure using a single slash). Since we don't
    // support movement of fields, we'll simply split the original apart and add it to our map
    // TODO: Allow users to enable support for mapping when the enclosure has already been mapped
    int fieldSeparator = elements[0].lastIndexOf('/');

    if (fieldSeparator == -1) {
      throw new IOException(
          "Illegal field mapping on line " + lineNumber + ": No field separator in source name");
    }

    String enclosure = elements[0].substring(0, fieldSeparator);
    String originalName = elements[0].substring(fieldSeparator + 1);

    fieldSeparator = elements[1].lastIndexOf('/');

    if (fieldSeparator == -1) {
      throw new IOException(
          "Illegal field mapping on line " + lineNumber + ": No field separator in target name");
    }

    String targetName = elements[1].substring(fieldSeparator + 1);
    mapping.fieldMappings.add(new FieldMapping(enclosure, originalName, targetName));
  }

  /**
   * Parses a method mapping SRG line.
   *
   * @param mapping a mapping instance.
   * @param lineNumber the current line number.
   * @param line an unparsed SRG line.
   * @throws IOException when parsing fails.
   */
  private void parseMethodMapping(@Nonnull NameMappingImpl mapping, int lineNumber,
      @Nonnull String line) throws IOException {
    String[] elements = line.split(" ");

    if (elements.length != 4) {
      throw new IOException("Illegal method mapping on line " + lineNumber
          + ": Expected exactly four parameters - Found " + elements.length);
    }

    // similarly to fields, SRG will give us a full name of the class and method separated by a
    // slash (e.g. we'll need to split them up again)
    int methodSeparator = elements[0].lastIndexOf('/');

    if (methodSeparator == -1) {
      throw new IOException(
          "Illegal method mapping on line " + lineNumber + ": No method separate in source name");
    }

    String enclosure = elements[0].substring(0, methodSeparator);
    String originalName = elements[0].substring(methodSeparator + 1);

    methodSeparator = elements[2].lastIndexOf('/');

    if (methodSeparator == -1) {
      throw new IOException(
          "Illegal method mapping on line " + lineNumber + ": No method separator in target name");
    }

    String targetName = elements[2].substring(methodSeparator + 1);
    mapping.methodMappings.add(new MethodMapping(
        enclosure,
        originalName,
        targetName,
        elements[1]
    ));
  }

  /**
   * Represents a parsed SRG mapping.
   */
  private static final class NameMappingImpl implements NameMapping {

    private final Map<String, String> classNameMap = new HashMap<>();
    private final Set<FieldMapping> fieldMappings = new HashSet<>();
    private final Set<MethodMapping> methodMappings = new HashSet<>();

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<String> getClassName(@NonNull String original) {
      return Optional.ofNullable(this.classNameMap.get(original));
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<String> getFieldName(@NonNull String className, @NonNull String fieldName,
        @NonNull String signature) {
      return this.fieldMappings.stream()
          .filter((m) -> m.matches(className, fieldName, signature))
          .findAny()
          .map(FieldMapping::getTargetName);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<String> getMethodName(@NonNull String className, @NonNull String methodName,
        @NonNull String signature) {
      return this.methodMappings.stream()
          .filter((m) -> m.matches(className, methodName, signature))
          .findAny()
          .map(MethodMapping::getTargetName);
    }
  }

  /**
   * Represents a single field mapping.
   */
  private static final class FieldMapping {

    private final String className;
    private final String originalName;
    private final String targetName;

    private FieldMapping(@Nonnull String className, @Nonnull String originalName,
        @Nonnull String targetName) {
      this.className = className;
      this.originalName = originalName;
      this.targetName = targetName;
    }

    @Nonnull
    public String getTargetName() {
      return this.targetName;
    }

    public boolean matches(@Nonnull String className, @Nonnull String name,
        @Nonnull String signature) {
      return this.className.equals(className) && this.originalName.equals(name);
    }
  }

  /**
   * Represents a single method mapping.
   */
  private static final class MethodMapping {

    private final String className;
    private final String originalName;
    private final String targetName;
    private final String signature;

    public MethodMapping(
        @Nonnull String className,
        @Nonnull String originalName,
        @Nonnull String targetName,
        @Nonnull String signature) {
      this.className = className;
      this.originalName = originalName;
      this.targetName = targetName;
      this.signature = signature;
    }

    @Nonnull
    public String getTargetName() {
      return this.targetName;
    }

    public boolean matches(@Nonnull String className, @Nonnull String name,
        @Nonnull String signature) {
      return this.className.equals(className) && this.originalName.equals(name) && this.signature
          .equals(signature);
    }
  }
}
