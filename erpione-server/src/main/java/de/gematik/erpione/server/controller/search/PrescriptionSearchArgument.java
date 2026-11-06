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

package de.gematik.erpione.server.controller.search;

import de.gematik.erpione.data.r4.etype.MedicationEType;
import de.gematik.erpione.data.r4.etype.PrescriptionEFlow;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Getter
@RequiredArgsConstructor
enum PrescriptionSearchArgument {
  FLOW_TYPE("flowType"),
  STATUS("status"),
  MEDICATION_TYPE("prescription.medication.type"),
  KVNR("prescription.patient.kvnr"),
  IKNR("prescription.coverage.iknr"),
  ;

  private final String path;

  public boolean matchesValue(String value) {
    return switch (this) {
      case FLOW_TYPE -> PrescriptionEFlow.matches(value);
      case STATUS -> PrescriptionEStatus.matches(value);
      case MEDICATION_TYPE -> MedicationEType.matches(value);
      default -> true;
    };
  }

  public String mapToValue(String value) {
    val mappedValue =
        switch (this) {
          case FLOW_TYPE -> PrescriptionEFlow.fromString(value).getCode();
          case STATUS -> PrescriptionEStatus.fromString(value).getCode();
          case MEDICATION_TYPE -> MedicationEType.fromString(value).getCode();
          default -> value;
        };

    return Optional.ofNullable(mappedValue)
        .orElseThrow(
            () -> new IllegalArgumentException("Invalid value for " + this.path + ": " + value));
  }

  public static Optional<PrescriptionSearchArgument> fromString(String param) {
    val unifiedParam = unifyParam(param);
    return Stream.of(values()).filter(p -> unifyParam(p.name()).equals(unifiedParam)).findFirst();
  }

  private static String unifyParam(String param) {
    return param.replaceAll("[^a-zA-Z]", "").toLowerCase().trim();
  }
}
