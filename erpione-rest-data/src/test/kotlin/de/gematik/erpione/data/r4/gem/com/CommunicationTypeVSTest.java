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

package de.gematik.erpione.data.r4.gem.com;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.of;

import de.gematik.test.erezept.fhir.r4.erp.ChargeItemCommunicationType;
import de.gematik.test.erezept.fhir.r4.erp.CommunicationType;
import de.gematik.test.erezept.fhir.r4.erp.ICommunicationType;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

class CommunicationTypeVSTest {

  @ParameterizedTest
  @MethodSource
  @NullSource
  void shouldThrowNotOnInvalidNames(String name) {
    val type = assertDoesNotThrow(() -> CommunicationEType.fromString(name));
    assertEquals(CommunicationEType.UNKNOWN, type);
  }

  static Stream<Arguments> shouldThrowNotOnInvalidNames() {
    return Stream.of("ABC", "").map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource
  void shouldMapFromTypeNames123(String name, CommunicationEType expected) {
    val actual = assertDoesNotThrow(() -> CommunicationEType.fromString(name));
    assertEquals(expected, actual);
  }

  static Stream<Arguments> shouldMapFromTypeNames123() {
    return Stream.of(
        of("INFO_REQ", CommunicationEType.INFO_REQ),
        of("info_req", CommunicationEType.INFO_REQ),
        of("DISP_REQ", CommunicationEType.DISP_REQ),
        of("REPLY", CommunicationEType.REPLY),
        of("DiGA", CommunicationEType.DIGA),
        of("DISP_REQ", CommunicationEType.DISP_REQ),
        of("REPRESENTATIVE", CommunicationEType.REPRESENTATIVE),
        of("change_REQ", CommunicationEType.CHANGE_REQ),
        of("change_REPLY", CommunicationEType.CHANGE_REPLY));
  }

  @ParameterizedTest
  @MethodSource
  void shouldMapFromFhirTypes(ICommunicationType<?> type, CommunicationEType expected) {
    val actual = assertDoesNotThrow(() -> CommunicationEType.fromString(type.name()));
    assertEquals(expected, actual);
  }

  static Stream<Arguments> shouldMapFromFhirTypes() {
    return Stream.of(
        of(CommunicationType.INFO_REQ, CommunicationEType.INFO_REQ),
        of(CommunicationType.DISP_REQ, CommunicationEType.DISP_REQ),
        of(CommunicationType.REPRESENTATIVE, CommunicationEType.REPRESENTATIVE),
        of(ChargeItemCommunicationType.CHANGE_REQ, CommunicationEType.CHANGE_REQ),
        of(ChargeItemCommunicationType.CHANGE_REPLY, CommunicationEType.CHANGE_REPLY));
  }
}
