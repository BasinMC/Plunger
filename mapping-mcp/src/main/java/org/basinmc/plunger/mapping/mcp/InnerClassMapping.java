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
package org.basinmc.plunger.mapping.mcp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class InnerClassMapping {

  private final Map<String, MappingEntry> mappings;
  private final Map<String, InnerClass> innerClasses;

  @JsonCreator
  private InnerClassMapping(@NonNull Map<String, MappingEntry> mappings) {
    this.mappings = mappings;

    this.innerClasses = mappings.entrySet().stream()
        .filter((e) -> e.getKey().contains("$"))
        .filter((e) -> !e.getValue().getInnerClasses().isEmpty())
        .flatMap((e) -> e.getValue().getInnerClasses().stream()
            .map((cl) -> new SimpleImmutableEntry<>(e.getKey(), cl)))
        .filter((e) -> e.getKey().equals(e.getValue().getInnerName()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  /**
   * Decodes a nested class mapping from the supplied file.
   *
   * @param path a file path.
   * @return a decoded class map.
   * @throws IOException when reading or decoding fails.
   */
  @NonNull
  public static InnerClassMapping read(@NonNull Path path) throws IOException {
    try (InputStream inputStream = Files.newInputStream(path)) {
      return read(inputStream);
    }
  }

  /**
   * Decodes a nested class mapping from the supplied input stream.
   *
   * @param inputStream an input stream.
   * @return a decoded class map.
   * @throws IOException when reading or decoding fails.
   */
  @NonNull
  public static InnerClassMapping read(@NonNull InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(inputStream, InnerClassMapping.class);
  }

  /**
   * Retrieves an inner class mapping from within this map.
   *
   * @param className a class name.
   * @return a mapping or an empty optional.
   */
  @NonNull
  public Optional<MappingEntry> getMapping(@NonNull String className) {
    return Optional.ofNullable(this.mappings.get(className));
  }

  /**
   * Retrieves the declaration of an inner class based on its name.
   *
   * @param className a class name.
   * @return an inner class or an empty optional.
   */
  @NonNull
  public Optional<InnerClass> getInnerClass(@NonNull String className) {
    return Optional.ofNullable(this.innerClasses.get(className));
  }

  /**
   * Represents an owner type (e.g. the parent of a type) as well as its method of origin (if any).
   */
  public static final class EnclosingMethod {

    private final String descriptor;
    private final String name;
    private final String owner;

    @JsonCreator
    private EnclosingMethod(
        @NonNull @JsonProperty(value = "owner", required = true) String owner,
        @Nullable @JsonProperty("name") String name,
        @Nullable @JsonProperty("desc") String descriptor) {
      this.owner = owner;
      this.name = name;
      this.descriptor = descriptor;
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
      EnclosingMethod that = (EnclosingMethod) o;
      return Objects.equals(this.owner, that.owner) &&
          Objects.equals(this.name, that.name) &&
          Objects.equals(this.descriptor, that.descriptor);
    }

    public String getDescriptor() {
      return this.descriptor;
    }

    public String getName() {
      return this.name;
    }

    public String getOwner() {
      return this.owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(this.owner, this.name, this.descriptor);
    }
  }

  /**
   * Represents a nested class (or the type itself if the class is an inner class itself and will
   * thus refer to itself in Bytecode).
   */
  public static class InnerClass {

    private final int access;
    private final String innerClass;
    private final String innerName;
    private final String outerClass;
    private final long start;

    @JsonCreator
    public InnerClass(
        @JsonProperty("access") String access,
        @NonNull @JsonProperty(value = "inner_class", required = true) String innerClass,
        @Nullable @JsonProperty("inner_name") String innerName,
        @Nullable @JsonProperty("outer_class") String outerClass,
        @JsonProperty("start") long start) {
      this.access = access == null ? 0 : Integer.parseInt(access, 16);
      this.innerClass = innerClass;
      this.innerName = innerName;
      this.outerClass = outerClass;
      this.start = start;
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
      InnerClass that = (InnerClass) o;
      return this.access == that.access &&
          this.start == that.start &&
          Objects.equals(this.innerClass, that.innerClass) &&
          Objects.equals(this.innerName, that.innerName) &&
          Objects.equals(this.outerClass, that.outerClass);
    }

    public int getAccess() {
      return this.access;
    }

    @NonNull
    public String getInnerClass() {
      return this.innerClass;
    }

    @Nullable
    public String getInnerName() {
      return this.innerName;
    }

    @Nullable
    public String getOuterClass() {
      return this.outerClass;
    }

    public long getStart() {
      return this.start;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects
          .hash(this.access, this.innerClass, this.innerName, this.outerClass, this.start);
    }
  }

  /**
   * Represents a mapping for a single class which identifies all of its inner classes as well as
   * its respective owner and method of origin.
   */
  public static final class MappingEntry {

    private final EnclosingMethod enclosingMethod;
    private final Set<InnerClass> innerClasses;

    private MappingEntry(
        @Nullable @JsonProperty("enclosingMethod") EnclosingMethod enclosingMethod,
        @NonNull @JsonProperty(value = "innerClasses", required = true) Set<InnerClass> innerClasses) {
      this.enclosingMethod = enclosingMethod;
      this.innerClasses = innerClasses;
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
      MappingEntry mapping = (MappingEntry) o;
      return Objects.equals(this.enclosingMethod, mapping.enclosingMethod) &&
          Objects.equals(this.innerClasses, mapping.innerClasses);
    }

    @NonNull
    public Optional<EnclosingMethod> getEnclosingMethod() {
      return Optional.ofNullable(this.enclosingMethod);
    }

    @NonNull
    public Set<InnerClass> getInnerClasses() {
      return Collections.unmodifiableSet(this.innerClasses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(this.enclosingMethod, this.innerClasses);
    }
  }
}
