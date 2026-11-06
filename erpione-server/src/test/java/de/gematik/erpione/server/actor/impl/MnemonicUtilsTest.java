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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MnemonicUtilsTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Machaon",
        "Podaleirios",
        "Telesphorus",
        "Hygieia",
        "Asclepius",
        "Epione",
        "Aceso",
        "Iaso",
        "Panacea"
      })
  void shouldNotCollideIds(String mnemonic) {
    val id = MnemonicUtils.mnemonicToId(mnemonic);
    assertNotNull(id, "Id should not be null");
  }

  @Test
  void shouldCompareCaseInsensitive() {
    val id1 = MnemonicUtils.mnemonicToId("Epione");
    val id2 = MnemonicUtils.mnemonicToId("epione");
    assertNotNull(id1, "Id1 should not be null");
    assertNotNull(id2, "Id2 should not be null");
    assertEquals(id1, id2, "Ids should be equal");
  }
}
