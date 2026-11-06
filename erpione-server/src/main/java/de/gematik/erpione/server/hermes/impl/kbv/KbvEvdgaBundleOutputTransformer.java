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

import de.gematik.erpione.data.r4.kbv.KbvEHealthAppRequestOut;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionOut;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.test.erezept.fhir.r4.kbv.KbvEvdgaBundle;
import lombok.val;

public class KbvEvdgaBundleOutputTransformer
    extends KbvBaseBundleOutputTransformer<KbvEvdgaBundle> {

  public KbvEvdgaBundleOutputTransformer(HermesService hermes) {
    super(hermes);
  }

  @Override
  public KbvEPrescriptionOut toJson(KbvEvdgaBundle resource) {
    val data = super.toJson(resource);

    // disassemble the health app request (HAR)
    val harr4 = resource.getHealthAppRequest();
    val pzn = harr4.getPzn().getValue();
    val name = harr4.getName();
    val ser = harr4.relatesToSocialCompensationLaw();
    val hardata = new KbvEHealthAppRequestOut(pzn, name, ser);

    data.setHealthAppRequest(hardata);

    return data;
  }

  @Override
  public Class<KbvEvdgaBundle> fhirType() {
    return KbvEvdgaBundle.class;
  }
}
