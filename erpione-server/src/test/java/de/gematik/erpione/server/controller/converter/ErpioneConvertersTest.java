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

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.gematik.erpione.data.actor.ActorERole;
import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.data.r4.etype.MedicationEType;
import de.gematik.erpione.data.r4.etype.PrescriptionEFlow;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ErpioneConvertersTest {

  static Stream<Arguments> shouldConvertMedicationType() {
    return MedicationEType.getEntries().stream()
        .flatMap(
            type -> Stream.of(type.getCode(), type.getDisplay()).map(it -> Arguments.of(it, type)));
  }

  static Stream<Arguments> shouldConvertActorRole() {
    return ActorERole.getEntries().stream()
        .flatMap(
            type -> Stream.of(type.getCode(), type.getDisplay()).map(it -> Arguments.of(it, type)));
  }

  static Stream<Arguments> shouldConvertInstitutionRole() {
    return InstitutionERole.getEntries().stream()
        .flatMap(
            type -> Stream.of(type.getCode(), type.getDisplay()).map(it -> Arguments.of(it, type)));
  }

  static Stream<Arguments> shouldConvertPrescriptionFlowType() {
    return PrescriptionEFlow.getEntries().stream()
        .flatMap(
            type -> Stream.of(type.getCode(), type.getDisplay()).map(it -> Arguments.of(it, type)));
  }

  @ParameterizedTest
  @MethodSource
  void shouldConvertMedicationType(String input, MedicationEType expected) {
    val converter = new ErpioneConverters.MedicationTypeConverter();

    val result1 = converter.convert(input);
    assertEquals(expected, result1);

    // case insensitivity
    val result2 = converter.convert(input.toLowerCase());
    assertEquals(expected, result2);

    val result3 = converter.convert(input.toUpperCase());
    assertEquals(expected, result3);
  }

  @ParameterizedTest
  @MethodSource
  void shouldConvertActorRole(String input, ActorERole expected) {
    val converter = new ErpioneConverters.ErpioneActorTypeConverter();

    val result1 = converter.convert(input);
    assertEquals(expected, result1);

    // case insensitivity
    val result2 = converter.convert(input.toLowerCase());
    assertEquals(expected, result2);

    val result3 = converter.convert(input.toUpperCase());
    assertEquals(expected, result3);
  }

  @ParameterizedTest
  @MethodSource
  void shouldConvertInstitutionRole(String input, InstitutionERole expected) {
    val converter = new ErpioneConverters.ErpioneInsitutionTypeConverter();

    val result1 = converter.convert(input);
    assertEquals(expected, result1);

    // case insensitivity
    val result2 = converter.convert(input.toLowerCase());
    assertEquals(expected, result2);

    val result3 = converter.convert(input.toUpperCase());
    assertEquals(expected, result3);
  }

  @ParameterizedTest
  @MethodSource
  void shouldConvertPrescriptionFlowType(String input, PrescriptionEFlow expected) {
    val converter = new ErpioneConverters.ErpionePrescriptionFlowTypeConverter();

    val result1 = converter.convert(input);
    assertEquals(expected, result1);

    // case insensitivity
    val result2 = converter.convert(input.toLowerCase());
    assertEquals(expected, result2);

    val result3 = converter.convert(input.toUpperCase());
    assertEquals(expected, result3);
  }
}
