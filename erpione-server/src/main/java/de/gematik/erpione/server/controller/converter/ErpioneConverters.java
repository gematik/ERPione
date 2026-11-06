/*
 * Copyright (Change Date see Readme) gematik GmbH
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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */

package de.gematik.erpione.server.controller.converter;

import de.gematik.erpione.data.actor.ActorERole;
import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.data.r4.etype.MedicationEType;
import de.gematik.erpione.data.r4.etype.PrescriptionEFlow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErpioneConverters {

  @Component
  @ReadingConverter
  public static class MedicationTypeConverter implements Converter<String, MedicationEType> {
    @Override
    public MedicationEType convert(@NotNull String source) {
      return MedicationEType.fromString(source);
    }

    @Bean
    @ConfigurationPropertiesBinding
    public MedicationTypeConverter medicationTypeConverter() {
      return new MedicationTypeConverter();
    }
  }

  @Component
  @ReadingConverter
  public static class ErpioneActorTypeConverter implements Converter<String, ActorERole> {
    @Override
    public ActorERole convert(@NotNull String source) {
      return ActorERole.fromStringOrThrow(source);
    }

    @Bean
    @ConfigurationPropertiesBinding
    public ErpioneActorTypeConverter erpioneActorTypeConverter() {
      return new ErpioneActorTypeConverter();
    }
  }

  @Component
  @ReadingConverter
  public static class ErpioneInsitutionTypeConverter
      implements Converter<String, InstitutionERole> {
    @Override
    public InstitutionERole convert(@NotNull String source) {
      return InstitutionERole.fromStringOrThrow(source);
    }

    @Bean
    @ConfigurationPropertiesBinding
    public ErpioneActorTypeConverter erpioneInstitutionTypeConverter() {
      return new ErpioneActorTypeConverter();
    }
  }

  @Component
  @ReadingConverter
  public static class ErpionePrescriptionFlowTypeConverter
      implements Converter<String, PrescriptionEFlow> {
    @Override
    public PrescriptionEFlow convert(@NotNull String source) {
      return PrescriptionEFlow.fromString(source);
    }

    @Bean
    @ConfigurationPropertiesBinding
    public ErpioneActorTypeConverter erpioneFlowTypeConverter() {
      return new ErpioneActorTypeConverter();
    }
  }
}
