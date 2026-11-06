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

package de.gematik.erpione.server.hermes.impl.kbv;

import static java.text.MessageFormat.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.gematik.erpione.data.r4.etype.SupplyEForm;
import de.gematik.test.erezept.fhir.valuesets.Darreichungsform;
import java.util.stream.Stream;
import lombok.val;
import org.junit.jupiter.api.Test;

public class SupplyEFormTransformationTest {

  @Test
  void shouldTransformSupplyEFormToFhir() {
    SupplyEForm.getEntries()
        .forEach(
            it -> {
              val df =
                  assertDoesNotThrow(
                      () -> Darreichungsform.fromCode(it.getCode()),
                      format(
                          "Failed to transform SupplyEForm code {0} to Darreichungsform",
                          it.getCode()));
              assertEquals(it.getCode(), df.getCode());
            });
  }

  @Test
  void shouldTransformDarreichungsformToHermes() {
    Stream.of(Darreichungsform.values())
        .forEach(
            it -> {
              val df =
                  assertDoesNotThrow(
                      () -> SupplyEForm.fromString(it.getCode()),
                      format(
                          "Failed to transform Darreichungsform code {0} to SupplyEForm",
                          it.getCode()));
              assertEquals(it.getCode(), df.getCode());
            });
  }
}
