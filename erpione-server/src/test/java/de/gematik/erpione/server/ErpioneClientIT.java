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

package de.gematik.erpione.server;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.erpione.client.ErpioneClientFactory;
import de.gematik.erpione.client.ErpioneRestException;
import de.gematik.erpione.client.rest.BasicRequests;
import de.gematik.erpione.client.rest.DoctorRequests;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    classes = ErpioneApplication.class,
    properties = {"spring.config.name=application,erpione", "erpione.online-mode=false"},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErpioneClientIT {

  @LocalServerPort private int serverPort;

  @Test
  void shouldInitializeClient() {
    assertDoesNotThrow(
        () -> ErpioneClientFactory.forRemote("http://localhost:" + serverPort).build(),
        "Client should be initialized and return valid version information");
  }

  @Test
  void shouldCallMetaInformation() {
    assertDoesNotThrow(
        () -> {
          val factory = ErpioneClientFactory.forRemote("http://localhost").port(serverPort).build();
          val info = factory.performBlockingOrThrow(BasicRequests.getInfo());
          val actors = factory.performBlockingOrThrow(BasicRequests.getActors());
          assertNotNull(info);
          assertNotNull(actors);
        });
  }

  @Test
  void shouldPrescribe() {
    assertDoesNotThrow(
        () -> {
          val factory = ErpioneClientFactory.forRemote("http://localhost").port(serverPort).build();
          val doc = factory.getDoctor("Machaon");
          val req = DoctorRequests.prescribe("X123123", false);
          assertThrows(ErpioneRestException.class, () -> doc.performBlockingOrThrow(req));
        });
  }
}
