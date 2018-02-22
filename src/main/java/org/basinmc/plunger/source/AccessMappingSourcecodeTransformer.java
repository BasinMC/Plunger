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

import java.nio.file.Path;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.basinmc.plunger.common.mapping.AccessFlag;
import org.basinmc.plunger.common.mapping.AccessMapping;
import org.basinmc.plunger.source.utility.ReferenceUtility;
import org.jboss.forge.roaster.model.FinalCapable;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.FinalCapableSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class AccessMappingSourcecodeTransformer extends AbstractCascadingSourcecodeTransformer {

  private final AccessMapping mapping;

  public AccessMappingSourcecodeTransformer(@Nonnull AccessMapping mapping) {
    this.mapping = mapping;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void transformField(@Nonnull Path source, @Nonnull JavaSource<?> typeSource,
      @Nonnull FieldSource<?> fieldSource) {
    AccessFlag flag = AccessFlag.byVisibility(fieldSource.getVisibility());

    if (fieldSource.isFinal()) {
      flag = flag.add(AccessFlag.FINAL);
    }

    this.mapping.getFieldAccessFlags(
        ReferenceUtility.getBytecodeReference(typeSource.getQualifiedName()),
        fieldSource.getName(),
        ReferenceUtility.getBytecodeTypeDescription(fieldSource.getType().getQualifiedName(),
            fieldSource.getType().getArrayDimensions()),
        flag)
        .ifPresent((f) -> {
          fieldSource.setVisibility(f.toVisibility());
          fieldSource.setFinal(f.contains(AccessFlag.FINAL));
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void transformMethod(@Nonnull Path source, @Nonnull JavaSource<?> typeSource,
      @Nonnull MethodSource<?> methodSource) {
    AccessFlag flag = AccessFlag.byVisibility(methodSource.getVisibility());

    if (methodSource.isFinal()) {
      flag = flag.add(AccessFlag.FINAL);
    }

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

    String signature = ReferenceUtility.generateBytecodeSignature(
        returnType,
        methodSource.getParameters().stream()
            .map((p) -> ReferenceUtility.getBytecodeTypeDescription(p.getType().getQualifiedName(),
                p.getType().getArrayDimensions()))
            .collect(Collectors.toList())
    );

    this.mapping.getMethodAccessFlags(
        ReferenceUtility.getBytecodeReference(typeSource.getQualifiedName()),
        name,
        signature,
        flag)
        .ifPresent((f) -> {
          methodSource.setVisibility(f.toVisibility());
          methodSource.setFinal(f.contains(AccessFlag.FINAL));
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void transformType(@Nonnull Path source, @Nonnull JavaSource<?> typeSource) {
    AccessFlag flag = AccessFlag.byVisibility(typeSource.getVisibility());

    if (typeSource instanceof FinalCapableSource && ((FinalCapable) typeSource).isFinal()) {
      flag = flag.add(AccessFlag.FINAL);
    }

    this.mapping
        .getClassAccessFlags(ReferenceUtility.getBytecodeReference(typeSource.getQualifiedName()),
            flag)
        .ifPresent((f) -> {
          typeSource.setVisibility(f.toVisibility());

          if (typeSource instanceof FinalCapableSource) {
            ((FinalCapableSource<?>) typeSource).setFinal(f.contains(AccessFlag.FINAL));
          }
        });
  }
}
