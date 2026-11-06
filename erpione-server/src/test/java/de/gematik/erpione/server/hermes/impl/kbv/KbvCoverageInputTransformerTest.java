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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.bbriccs.fhir.de.value.IKNR;
import de.gematik.bbriccs.fhir.de.valueset.InsuranceTypeDe;
import de.gematik.erpione.data.r4.etype.InsuranceEType;
import de.gematik.erpione.data.r4.kbv.KbvECoverageIn;
import de.gematik.erpione.data.r4.kbv.KbvECoverageOut;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.test.erezept.fhir.builder.kbv.KbvCoverageFaker;
import de.gematik.test.erezept.fhir.r4.kbv.KbvCoverage;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=rudev,offline,noauth"})
class KbvCoverageInputTransformerTest {

  @Autowired private HermesServiceImpl hermes;

  @Test
  void shouldTranslateCustomExample() {
    val data = new KbvECoverageIn();
    data.setIknr(IKNR.randomStringValue());

    val encoded = hermes.toFhirString(KbvCoverage.class, data);

    val resource = assertDoesNotThrow(() -> hermes.decode(KbvCoverage.class, encoded));
    assertTrue(resource.getIknr().isPresent());
    assertEquals(data.getIknr(), resource.getIknr().map(IKNR::getValue).orElse(null));
  }

  @Test
  void shouldTranslateFhirToJson() {
    val resource = KbvCoverageFaker.builder().withInsuranceType(InsuranceTypeDe.GKV).fake();

    val encoded = hermes.toJson(KbvECoverageOut.class, resource);

    assertEquals(InsuranceEType.GKV, encoded.getType());
  }
}
