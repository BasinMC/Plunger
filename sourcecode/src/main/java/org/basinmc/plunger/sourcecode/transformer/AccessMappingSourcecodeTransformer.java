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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.file.Path;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.basinmc.plunger.mapping.AccessFlag;
import org.basinmc.plunger.mapping.AccessMapping;
import org.basinmc.plunger.sourcecode.utility.ReferenceUtility;
import org.jboss.forge.roaster.model.FinalCapable;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.FinalCapableSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * <p>Alters the access level of classes, fields and methods based on an internal set of access
 * mappings.</p>
 *
 * <p>FIXME: Implemented proper support for inner classes Source mapping does not correctly resolve
 * inner classes at the moment due to the fact that we cannot differentiate between inner and
 * regular classes based on the information Roaster gives us here. As such, the naming for inner
 * classes will generally be incorrectly inferred and fail to look up correctly (e.g. should be
 * {@code org/basinmc/plunger/test/TestClass$InnerClass} but actually is {@code
 * org/basinmc/plunger/test/TestClass/InnerClass} as the type is accessed equally to a type nested
 * within a package.</p>
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class AccessMappingSourcecodeTransformer extends AbstractCascadingSourcecodeTransformer {

  private final AccessMapping mapping;

  public AccessMappingSourcecodeTransformer(@Nonnull AccessMapping mapping) {
    this.mapping = mapping;
  }

  @NonNull
  private static AccessFlag byVisibility(@NonNull Visibility visibility) {
    switch (visibility) {
      case PUBLIC:
        return AccessFlag.PUBLIC;
      case PROTECTED:
        return AccessFlag.PROTECTED;
      case PRIVATE:
        return AccessFlag.PRIVATE;
      case PACKAGE_PRIVATE:
        return AccessFlag.PACKAGE_PRIVATE;
    }

    return AccessFlag.NONE;
  }

  @NonNull
  private static Visibility toVisibility(@NonNull AccessFlag flag) {
    if (flag.contains(AccessFlag.PUBLIC)) {
      return Visibility.PUBLIC;
    } else if (flag.contains(AccessFlag.PROTECTED)) {
      return Visibility.PROTECTED;
    } else if (flag.contains(AccessFlag.PACKAGE_PRIVATE)) {
      return Visibility.PACKAGE_PRIVATE;
    } else if (flag.contains(AccessFlag.PRIVATE)) {
      return Visibility.PRIVATE;
    }

    throw new IllegalStateException("Cannot convert to visibility: No visibility flags set");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void transformField(@Nonnull Path source, @Nonnull JavaSource<?> typeSource,
      @Nonnull FieldSource<?> fieldSource) {
    AccessFlag flag = byVisibility(fieldSource.getVisibility());

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
          fieldSource.setVisibility(toVisibility(f));
          fieldSource.setFinal(f.contains(AccessFlag.FINAL));
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void transformMethod(@Nonnull Path source, @Nonnull JavaSource<?> typeSource,
      @Nonnull MethodSource<?> methodSource) {
    AccessFlag flag = byVisibility(methodSource.getVisibility());

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
          methodSource.setVisibility(toVisibility(f));
          methodSource.setFinal(f.contains(AccessFlag.FINAL));
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void transformType(@Nonnull Path source, @Nonnull JavaSource<?> typeSource) {
    AccessFlag flag = byVisibility(typeSource.getVisibility());

    if (typeSource instanceof FinalCapableSource && ((FinalCapable) typeSource).isFinal()) {
      flag = flag.add(AccessFlag.FINAL);
    }

    this.mapping
        .getClassAccessFlags(ReferenceUtility.getBytecodeReference(typeSource.getQualifiedName()),
            flag)
        .ifPresent((f) -> {
          typeSource.setVisibility(toVisibility(f));

          if (typeSource instanceof FinalCapableSource) {
            ((FinalCapableSource<?>) typeSource).setFinal(f.contains(AccessFlag.FINAL));
          }
        });
  }
}
