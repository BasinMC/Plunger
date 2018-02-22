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
package org.basinmc.plunger.source;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.basinmc.plunger.source.generator.JavaDocGenerator;
import org.basinmc.plunger.source.utility.ReferenceUtility;
import org.jboss.forge.roaster.model.JavaDocCapable;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.FieldHolderSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaDocCapableSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodHolderSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * Replaces the JavaDoc of all transformed source files.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class JavaDocSourceCodeTransformer implements SourceCodeTransformer {

  private final JavaDocGenerator generator;

  public JavaDocSourceCodeTransformer(@Nonnull JavaDocGenerator generator) {
    this.generator = generator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void transform(@NonNull Path source, @NonNull JavaSource<?> sourceType) {
    this.transform(sourceType);
  }

  private void transform(@Nonnull JavaType<?> type) {
    if (type instanceof JavaDocCapableSource) {
      this.generator
          .getClassDocumentation(ReferenceUtility.getBytecodeReference(type.getQualifiedName()))
          .ifPresent(
              (doc) -> ((JavaDocSource) ((JavaDocCapable) type).getJavaDoc()).setFullText(doc));
    }

    if (type instanceof FieldHolderSource) {
      ((FieldHolderSource<?>) type).getFields().forEach((field) -> this.transform(
          type.getQualifiedName(),
          field
      ));
    }

    if (type instanceof MethodHolderSource) {
      ((MethodHolderSource<?>) type).getMethods().forEach((method) -> this.transform(
          type.getQualifiedName(),
          method
      ));
    }
  }

  private void transform(@Nonnull String className, @Nonnull FieldSource<?> fieldSource) {
    this.generator.getFieldDocumentation(
        ReferenceUtility.getBytecodeReference(className),
        fieldSource.getName(),
        ReferenceUtility.getBytecodeTypeDescription(fieldSource.getType().getQualifiedName(),
            fieldSource.getType().getArrayDimensions()))
        .ifPresent((doc) -> fieldSource.getJavaDoc().setFullText(doc));
  }

  private void transform(@Nonnull String className, @Nonnull MethodSource<?> methodSource) {
    String returnType = ReferenceUtility.VOID_REFERENCE;
    String name = methodSource.getName();

    if (methodSource.isConstructor()) {
      name = ReferenceUtility.CONSTRUCTOR_NAME;
    }

    if (methodSource.getReturnType() != null) {
      returnType = ReferenceUtility
          .getBytecodeTypeDescription(methodSource.getReturnType().getQualifiedName(),
              methodSource.getReturnType().getArrayDimensions());
    }

    this.generator.getMethodDocumentation(
        ReferenceUtility.getBytecodeReference(className),
        name,
        ReferenceUtility.generateBytecodeSignature(
            returnType,
            methodSource.getParameters().stream()
                .map((p) -> ReferenceUtility
                    .getBytecodeTypeDescription(p.getType().getQualifiedName(),
                        p.getType().getArrayDimensions()))
                .collect(Collectors.toList())
        ))
        .ifPresent((doc) -> methodSource.getJavaDoc().setFullText(doc));
  }
}
