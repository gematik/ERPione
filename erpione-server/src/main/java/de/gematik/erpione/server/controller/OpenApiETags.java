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

package de.gematik.erpione.server.controller;

import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OpenApiETags {

  public static final String META_INFO_TAG = "Meta Information";
  public static final String ERP_TAG = "E-Rezept";
  public static final String DOCTOR_TAG = "Gesundheitseinrichtung";
  public static final String PHARMACY_TAG = "Apotheke";
  public static final String EU_PHARMACY_TAG = "EU Apotheke";
  public static final String KTR_TAG = "Kostenträger";
  public static final String CHARGE_ITEM_TAG = "Kostenbeleg";
  public static final String COMMUNICATION_TAG = "Nachrichten";
  public static final String FAKER_TAG = "Faker";
  public static final String PRESCRIPTION_DB_TAG = "Prescription Database";

  public static List<Tag> getAllTags() {
    return List.of(
        new Tag()
            .name(META_INFO_TAG)
            .description("Meta information about the API and its configuration"),
        new Tag().name(ERP_TAG).description("Use cases related to the E-Rezept system"),
        new Tag().name(DOCTOR_TAG).description("Use cases which can be performed by a doctor"),
        new Tag()
            .name(PHARMACY_TAG)
            .description("Use cases which can be performed by a pharmacist"),
        new Tag()
            .name(EU_PHARMACY_TAG)
            .description(
                "Use cases related to the NCPeH and E-Rezept systems for EU prescription handling"),
        new Tag()
            .name(KTR_TAG)
            .description("Use cases which can be performed by a health insurance provider"),
        new Tag()
            .name(CHARGE_ITEM_TAG)
            .description(
                "Use cases related to the handling of charge items and billing information"),
        new Tag()
            .name(FAKER_TAG)
            .description("Use cases related to the Hermes Faker functionality"),
        new Tag()
            .name(PRESCRIPTION_DB_TAG)
            .description("Use cases related to the Prescription Database functionality"),
        new Tag()
            .name(COMMUNICATION_TAG)
            .description("Use cases related to the handling of communications and messages"));
  }
}
