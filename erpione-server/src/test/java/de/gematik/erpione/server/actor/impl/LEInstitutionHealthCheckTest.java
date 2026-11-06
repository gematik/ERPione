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

package de.gematik.erpione.server.actor.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.rest.HttpBResponse;
import de.gematik.erpione.data.meta.TIHealthCheckStatus;
import de.gematik.erpione.server.ErpioneApplication;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import de.gematik.erpione.server.util.TestActorsFactory;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = {ErpioneApplication.class, ErpEnvironmentConfig.class, TestActorsFactory.class},
    properties = {"spring.profiles.active=rudev,offline,noauth"})
class LEInstitutionHealthCheckTest {

  @Autowired private TestActorsFactory testActorsFactory;

  @Test
  void shouldPerformSimpleHealthCheck() {
    val pair = testActorsFactory.getTestPharmacyForHealthCheck("Hygieia");
    val erpProbingClient = pair.getFirst();
    val actor = pair.getSecond();

    when(erpProbingClient.send(any())).thenReturn(HttpBResponse.status(200).withoutPayload());

    val result = assertDoesNotThrow(actor::erpHealthCheck);
    assertNotNull(result);
    assertEquals(TIHealthCheckStatus.OK, result.getStatus());
    assertEquals(200, result.getStatusCode());
  }

  @Test
  void shouldRefreshIdpToken() {
    // this one is only for test coverage, as the actual token refresh is handled by the ErpClient
    // and not by the LEInstitution
    val pair = testActorsFactory.getTestPharmacy("Hygieia");
    val actor = pair.getSecond();
    assertDoesNotThrow(actor::refreshIdpToken);
  }
}
