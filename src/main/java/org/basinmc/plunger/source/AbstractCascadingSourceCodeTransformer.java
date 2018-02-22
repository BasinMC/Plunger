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
import javax.annotation.Nonnull;
import org.jboss.forge.roaster.model.source.FieldHolderSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodHolderSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.TypeHolderSource;

/**
 * Provides a transformer implementation which iterates over a hierarchy of nested classes until the
 * list of members is exhausted.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public abstract class AbstractCascadingSourceCodeTransformer implements SourceCodeTransformer {

  /**
   * {@inheritDoc}
   */
  @Override
  public void transform(@NonNull Path source, @NonNull JavaSource<?> sourceType) {
    this.transformType(source, sourceType);

    if (sourceType instanceof FieldHolderSource) {
      FieldHolderSource<?> fieldHolderSource = (FieldHolderSource) sourceType;
      fieldHolderSource.getFields().forEach((f) -> this.transformField(source, sourceType, f));
    }

    if (sourceType instanceof MethodHolderSource) {
      MethodHolderSource<?> methodHolderSource = (MethodHolderSource) sourceType;
      methodHolderSource.getMethods().forEach((m) -> this.transformMethod(source, sourceType, m));
    }

    if (sourceType instanceof TypeHolderSource) {
      TypeHolderSource<?> typeHolderSource = (TypeHolderSource) sourceType;
      typeHolderSource.getNestedTypes().forEach((n) -> this.transform(source, n));
    }
  }

  protected void transformType(@Nonnull Path source, @Nonnull JavaSource<?> typeSource) {
  }

  protected void transformField(@Nonnull Path source, @Nonnull JavaSource<?> typeSource,
      @Nonnull FieldSource<?> fieldSource) {
  }

  protected void transformMethod(@Nonnull Path source, @Nonnull JavaSource<?> typeSource,
      @Nonnull MethodSource<?> methodSource) {
  }
}
