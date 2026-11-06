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

import de.gematik.erpione.data.r4.kbv.*;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.impl.FhirToJsonTransformer;
import de.gematik.test.erezept.fhir.r4.kbv.*;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public abstract class KbvBaseBundleOutputTransformer<R extends KbvBaseBundle>
    implements FhirToJsonTransformer<KbvEPrescriptionOut, R> {

  protected final HermesService hermes;

  protected KbvEPractitionerRoleOut practitionerRoleToJson(KbvPractitionerRole roleResource) {
    return new KbvEPractitionerRoleOut(roleResource.getTeamNumber().getValue());
  }

  @Override
  public KbvEPrescriptionOut toJson(R resource) {
    val pid = resource.getPrescriptionId().getValue();

    val patient = hermes.toJson(KbvEPatientOut.class, resource.getPatient());
    val coverage = hermes.toJson(KbvECoverageOut.class, resource.getCoverage());
    val practitioner = hermes.toJson(KbvEPractitionerOut.class, resource.getPractitioner());
    val organization = hermes.toJson(KbvEOrganizationOut.class, resource.getMedicalOrganization());
    val practitionerRole =
        resource.getPractitionerRole().map(this::practitionerRoleToJson).stream()
            .findFirst()
            .orElse(null);

    return new KbvEPrescriptionOut(
        pid, patient, null, coverage, null, null, practitioner, practitionerRole, organization);
  }

  @Override
  public final Class<KbvEPrescriptionOut> jsonType() {
    return KbvEPrescriptionOut.class;
  }
}
