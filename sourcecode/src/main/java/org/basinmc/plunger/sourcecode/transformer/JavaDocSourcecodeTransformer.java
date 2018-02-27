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
package org.basinmc.plunger.sourcecode.transformer;

import java.nio.file.Path;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.basinmc.plunger.sourcecode.generator.JavaDocGenerator;
import org.basinmc.plunger.sourcecode.utility.ReferenceUtility;
import org.basinmc.plunger.sourcecode.generator.JavaDocGenerator;
import org.basinmc.plunger.sourcecode.utility.ReferenceUtility;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * Replaces the JavaDoc of all transformed source files.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class JavaDocSourcecodeTransformer extends AbstractCascadingSourcecodeTransformer {

  private final JavaDocGenerator generator;

  public JavaDocSourcecodeTransformer(@Nonnull JavaDocGenerator generator) {
    this.generator = generator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void transformType(@Nonnull Path source, @Nonnull JavaSource<?> typeSource) {
    this.generator
        .getClassDocumentation(
            ReferenceUtility.getBytecodeReference(typeSource.getQualifiedName()))
        .ifPresent((doc) -> typeSource.getJavaDoc().setFullText(doc));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void transformField(@Nonnull Path source, @Nonnull JavaSource<?> typeSource,
      @Nonnull FieldSource<?> fieldSource) {
    this.generator.getFieldDocumentation(
        ReferenceUtility.getBytecodeReference(typeSource.getQualifiedName()),
        fieldSource.getName(),
        ReferenceUtility.getBytecodeTypeDescription(fieldSource.getType().getQualifiedName(),
            fieldSource.getType().getArrayDimensions()))
        .ifPresent((doc) -> fieldSource.getJavaDoc().setFullText(doc));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void transformMethod(@Nonnull Path source, @Nonnull JavaSource<?> typeSource,
      @Nonnull MethodSource<?> methodSource) {
    String returnType = ReferenceUtility.VOID_REFERENCE;
    String name = methodSource.getName();

    if (methodSource.isConstructor()) {
      name = ReferenceUtility.CONSTRUCTOR_NAME;
    }

    if (methodSource.getReturnType() != null) {
      Type<?> type = methodSource.getReturnType();

      returnType = ReferenceUtility.getBytecodeTypeDescription(
          type.isPrimitive() ? type.getName() : type.getQualifiedName(),
          type.getArrayDimensions()
      );
    }

    this.generator.getMethodDocumentation(
        ReferenceUtility.getBytecodeReference(typeSource.getQualifiedName()),
        name,
        ReferenceUtility.generateBytecodeSignature(
            returnType,
            methodSource.getParameters().stream()
                .map((p) -> {
                  Type<?> type = p.getType();

                  return ReferenceUtility.getBytecodeTypeDescription(
                      type.isPrimitive() ? type.getName() : type.getQualifiedName(),
                      type.getArrayDimensions()
                  );
                })
                .collect(Collectors.toList())
        ))
        .ifPresent((doc) -> methodSource.getJavaDoc().setFullText(doc));
  }
}
