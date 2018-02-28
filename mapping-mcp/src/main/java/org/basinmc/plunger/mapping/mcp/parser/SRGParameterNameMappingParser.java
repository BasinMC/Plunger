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
package org.basinmc.plunger.mapping.mcp.parser;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.basinmc.plunger.mapping.ParameterNameMapping;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class SRGParameterNameMappingParser {

  @NonNull
  public ParameterNameMapping parse(@NonNull InputStream inputStream) throws IOException {
    Properties properties = new Properties();
    properties.load(inputStream);
    return this.parse(properties);
  }

  @NonNull
  public ParameterNameMapping parse(@NonNull Path path) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      return this.parse(reader);
    }
  }

  @NonNull
  public ParameterNameMapping parse(@NonNull Reader reader) throws IOException {
    Properties properties = new Properties();
    properties.load(reader);
    return this.parse(properties);
  }

  @NonNull
  private ParameterNameMapping parse(@NonNull Properties properties) throws IOException {
    ParameterNameMappingImpl mapping = new ParameterNameMappingImpl();

    for (Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();

      // we're really not interested in this value at the moment
      if ("max_constructor_index".equals(key)) {
        continue;
      }

      // while SRGs are generally pretty simple in their formatting and efficient to read, this one
      // is not. Effectively these mappings are made up of a set of properties which map the full
      // signature of a method to its parameter names where each parameter is separated by a comma.
      // class name and method name are separated by a dot while the method signature is passed
      // as-is
      int classNameSeparatorIndex = key.lastIndexOf('.');
      if (classNameSeparatorIndex == -1) {
        throw new IOException("Illegal record \"" + key + "\": No class name separator in key");
      }

      String className = key.substring(0, classNameSeparatorIndex);
      key = key.substring(classNameSeparatorIndex + 1);

      int methodSignatureSeparatorIndex = key.indexOf('(');
      if (methodSignatureSeparatorIndex == -1) {
        throw new IOException("Illegal record \"" + key + "\" in class " + className
            + ": No method signature separator in key");
      }

      String methodName = key.substring(0, methodSignatureSeparatorIndex);
      String signature = key.substring(methodSignatureSeparatorIndex);

      // in case of more specialized mappings, we'll simply ignore them as we only care for
      // parameters in this particular implementation
      if (signature.contains("-")) {
        continue;
      }

      // values may also contain an exception signature (which we obviously ignore in this mapper
      // implementation) so we'll simply strip that part
      int parameterSeparatorIndex = value.lastIndexOf('|');

      if (parameterSeparatorIndex == -1) {
        throw new IOException(
            "Illegal record value \"" + value + "\" in class " + className + " for method "
                + methodName + signature + ": No parameter separator in value");
      }

      value = value.substring(parameterSeparatorIndex + 1);

      // since mappings which purely define exceptions may not provide an actual parameter mapping,
      // we'll simply ignore them here
      if (value.isEmpty()) {
        continue;
      }

      String[] parameterNames = value.split(",");

      for (int i = 0; i < parameterNames.length; ++i) {
        mapping.entries.add(new ParameterNameMappingEntry(
            className,
            methodName,
            signature,
            i,
            parameterNames[i]
        ));
      }
    }
    return mapping;
  }

  private static final class ParameterNameMappingImpl implements ParameterNameMapping {

    private final Set<ParameterNameMappingEntry> entries = new HashSet<>();

    private ParameterNameMappingImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Optional<String> getParameterName(@NonNull String className, @NonNull String methodName,
        @NonNull String signature, @Nullable String parameterName, int parameterIndex) {
      return this.entries.stream()
          .filter((e) -> e.matches(className, methodName, signature, parameterIndex))
          .map(ParameterNameMappingEntry::getTargetName)
          .findAny();
    }
  }

  private static final class ParameterNameMappingEntry {

    private final String className;
    private final String methodName;
    private final String signature;
    private final int parameterIndex;
    private final String targetName;

    private ParameterNameMappingEntry(
        @NonNull String className,
        @NonNull String methodName,
        @NonNull String signature,
        int parameterIndex,
        @NonNull String targetName) {
      this.className = className;
      this.methodName = methodName;
      this.signature = signature;
      this.parameterIndex = parameterIndex;
      this.targetName = targetName;
    }

    @NonNull
    public String getTargetName() {
      return this.targetName;
    }

    public boolean matches(@NonNull String className, @NonNull String methodName,
        @NonNull String signature, int parameterIndex) {
      return this.className.equals(className) &&
          this.methodName.equals(methodName) &&
          this.signature.equals(signature) &&
          this.parameterIndex == parameterIndex;
    }
  }
}
