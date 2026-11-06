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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.server.actor.error.ActorNotFoundError;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class OfflineControllersTest {

  @Autowired private MetaController metaController;
  @Autowired private ActorStageController actorsController;

  @Test
  void shouldGetAllActors() {
    val actors = actorsController.getActors(null);
    assertNotNull(actors, "Actors list must not be null");
    assertFalse(actors.isEmpty(), "Actors list must not be empty");
  }

  @ParameterizedTest
  @EnumSource(value = InstitutionERole.class)
  void shouldGetAllActorsByRole(InstitutionERole role) {
    val actors = actorsController.getActors(role);
    assertNotNull(actors, "Actors list must not be null");
    assertFalse(actors.isEmpty(), "Actors list must not be empty");
  }

  @Test
  void shouldGetEachInstitutionByMnemonicId() {
    actorsController
        .getActors(null)
        .forEach(
            actor -> {
              assertDoesNotThrow(() -> actorsController.getActorById(actor.getMnemonicId()));
              assertDoesNotThrow(() -> actorsController.getActorById(actor.getMnemonic()));
            });
  }

  @Test
  void shouldThrowOnUnknownActor() {
    assertThrows(ActorNotFoundError.class, () -> actorsController.getActorById("unknown"));
  }

  @Test
  void shouldReadMetaInformation() {
    val info = assertDoesNotThrow(() -> metaController.getErpioneInfo());
    assertNotNull(info.getTi());
    assertNotNull(info.getFhir());
    assertNotNull(info.getBuild());
  }
}
