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

package de.gematik.erpione.server.hermes.impl;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.erpione.data.r4.HumanENameIn;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class HermesUtilsTest {

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t", "\n"})
  @NullSource
  void shouldDetectNullOrEmptyString(String input) {
    assertTrue(HermesUtils.ofNullable(input).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"hello world", " abc", "\tdef", "\nghi"})
  void shouldDetectNonEmptyString(String input) {
    assertTrue(HermesUtils.ofNullable(input).isPresent());
  }

  @Test
  void shouldReplaceNullObject() {
    val result = assertDoesNotThrow(() -> HermesUtils.requireNonNullElseGet(null, () -> 42));
    assertEquals(42, result);
  }

  @Test
  void shouldKeepExplicitPrefixInCompleteName() {
    val nameIn = new HumanENameIn("Dr. med.", "Ludger", "Königsstein");
    val out = HermesUtils.completeName(nameIn);
    assertEquals("Dr. med.", out.getPrefix());
    assertEquals("Ludger", out.getGiven());
    assertEquals("Königsstein", out.getFamily());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  @NullSource
  void shouldCompleteNameFromMissingInput(String blankPrefix) {
    val nameIn = new HumanENameIn(blankPrefix, null, null);
    val out = HermesUtils.completeName(nameIn);
    assertFalse(out.getGiven().isBlank());
    assertFalse(out.getFamily().isBlank());
    if (out.getPrefix() != null) {
      assertFalse(out.getPrefix().isBlank());
    }
  }

  @Test
  void shouldCompleteNameFromNull() {
    val out = HermesUtils.completeName(null);
    assertNotNull(out.getGiven());
    assertNotNull(out.getFamily());
  }
}
