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
package org.basinmc.plunger.sourcecode.utility;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Provides a utility implementation which permits the conversion between Java language and JVM
 * references of class names, field and method signatures.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public final class ReferenceUtility {

  /**
   * Defines the bytecode name for class constructors.
   */
  public static final String CONSTRUCTOR_NAME = "<init>";
  /**
   * Defines a reference to the void type.
   */
  public static final String VOID_REFERENCE = "V";
  /**
   * Provides a map which converts the names of primitives into their respective Bytecode names.
   */
  private static final Map<String, Character> PRIMITIVE_REFERENCE_MAP = Stream.of(
      new SimpleImmutableEntry<>("byte", 'B'),
      new SimpleImmutableEntry<>("char", 'C'),
      new SimpleImmutableEntry<>("double", 'D'),
      new SimpleImmutableEntry<>("float", 'F'),
      new SimpleImmutableEntry<>("int", 'I'),
      new SimpleImmutableEntry<>("long", 'J'),
      new SimpleImmutableEntry<>("short", 'S'),
      new SimpleImmutableEntry<>("boolean", 'Z'),
      new SimpleImmutableEntry<>("void", 'V')
  ).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  /**
   * Provides a map which converts the Bytecode names to their respective language names.
   */
  private static final Map<Character, String> REVERSE_PRIMITIVE_REFERENCE_MAP =
      PRIMITIVE_REFERENCE_MAP.entrySet().stream()
          .collect(Collectors.toMap(Entry::getValue, Entry::getKey));

  private ReferenceUtility() {
  }

  /**
   * Generates a method signature using the specified Bytecode references for return type and
   * parameter types.
   *
   * @param returnType a Bytecode signature for the return type.
   * @param parameterTypes an ordered list of Bytecode signatures for each parameter of the method.
   * @return a method signature.
   */
  @NonNull
  public static String generateBytecodeSignature(@NonNull String returnType,
      @NonNull List<String> parameterTypes) {
    StringBuilder builder = new StringBuilder();

    parameterTypes.forEach((r) -> {
      if (builder.length() != 0) {
        builder.append(";");
      }

      builder.append(r);
    });

    return "(" + builder + ")" + returnType;
  }

  /**
   * Retrieves the dimensions of an array based on a Bytecode signature for a specific type (such as
   * a field type or method return value).
   *
   * @param signature a Bytecode signature.
   * @return an array depth (or zero when the signature does not define an array).
   */
  public static int getArrayDimension(@NonNull String signature) {
    int dimension = 0;

    while (signature.startsWith("[")) {
      ++dimension;
      signature = signature.substring(1);
    }

    return dimension;
  }

  /**
   * Extracts the Bytecode definitions for the parameters within a specified method signature.
   *
   * @param signature a signature.
   * @return a list of Bytecode references.
   */
  @NonNull
  public static List<String> getBytecodeParameterReferences(@NonNull String signature) {
    // strip the first character and remove the return type
    signature = signature.substring(1);
    signature = signature.substring(0, signature.lastIndexOf(')'));

    // split the parameters and parse them into their correct representation
    return Arrays.asList(signature.split(";"));
  }

  /**
   * <p>Converts a fully qualified reference to a class into its standalone Bytecode reference.</p>
   *
   * <p>Note that this method does not handle the conversion of primitives or its respective
   * prefixing (as typically seen within method and field signatures). Use {@link
   * #getBytecodeTypeDescription(String)} for these purposes.</p>
   *
   * @param className a fully qualified class name.
   * @return a fully qualified Bytecode reference.
   */
  @NonNull
  public static String getBytecodeReference(@NonNull String className) {
    return className.replace('.', '/');
  }

  /**
   * Extracts the Bytecode reference for a method's return type from a specified method signature.
   *
   * @param signature a signature.
   * @return a return type Bytecode reference.
   */
  @NonNull
  public static String getBytecodeReturnTypeReference(@NonNull String signature) {
    return signature.substring(0, signature.lastIndexOf(')'));
  }

  /**
   * Converts a fully qualified signature (such as a field signature) into its Bytecode reference.
   *
   * @param signature a fully qualified type reference or primitive type name.
   * @return a Bytecode signature.
   */
  @NonNull
  public static String getBytecodeTypeDescription(@NonNull String signature) {
    if (PRIMITIVE_REFERENCE_MAP.containsKey(signature)) {
      return Character.toString(PRIMITIVE_REFERENCE_MAP.get(signature));
    }

    return "L" + getBytecodeReference(signature);
  }

  /**
   * Converts a fully qualified signature (such as a field signature) into its Bytecode reference.
   *
   * @param signature a fully qualified type reference or primitive type name.
   * @param arrayDimension an array dimension (non-negative).
   * @return a Bytecode signature.
   */
  @NonNull
  public static String getBytecodeTypeDescription(@NonNull String signature, int arrayDimension) {
    StringBuilder builder = new StringBuilder();

    for (int i = 1; i < arrayDimension; ++i) {
      builder.append("[");
    }

    builder.append(getBytecodeTypeDescription(signature));
    return builder.toString();
  }

  /**
   * Converts a fully qualified Bytecode signature into its Java type representation (or name of
   * primitive).
   *
   * @param reference a full Bytecode reference.
   * @return a fully qualified Java type reference.
   */
  @NonNull
  public static String getJavaTypeDescription(@NonNull String reference) {
    // strip the array attributes as we do not handle them here
    while (reference.startsWith("[")) {
      reference = reference.substring(1);
    }

    char typePrefix = reference.charAt(0);
    reference = reference.substring(1);

    if (typePrefix == 'L') {
      return getJavaTypeReference(reference);
    }

    return REVERSE_PRIMITIVE_REFERENCE_MAP.getOrDefault(typePrefix, "unknown");
  }

  /**
   * <p>Converts a fully qualified Bytecode class reference into its Java representation.</p>
   *
   * <p>Note that this method does not handle the conversion of primitives or the prefixing of class
   * names (as typically seen within method and field signatures). Use {@link
   * #getJavaTypeDescription(String)} for full signatures.</p>
   *
   * @param reference a fully qualified Bytecode reference.
   * @return a fully qualified Java reference.
   */
  @NonNull
  public static String getJavaTypeReference(@NonNull String reference) {
    return reference.replace('/', '.');
  }
}
