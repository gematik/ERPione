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

package de.gematik.erpione.server.controller.faker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import de.gematik.erpione.data.r4.etype.PrescriptionEFlow;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class GemFakerControllerTest {

  @Autowired private GemFakerController controller;

  @ParameterizedTest
  @EnumSource(PrescriptionEFlow.class)
  @NullSource
  void shouldFakePrescriptionFlowTypeParameters(PrescriptionEFlow flowType) {
    assertDoesNotThrow(() -> controller.createPrescriptionFlowType(flowType));
  }
}
