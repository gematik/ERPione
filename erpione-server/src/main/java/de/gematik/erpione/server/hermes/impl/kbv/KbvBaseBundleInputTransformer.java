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

import static java.util.Optional.ofNullable;

import de.gematik.bbriccs.fhir.coding.version.ProfileVersion;
import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.erpione.data.r4.kbv.*;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.impl.JsonToFhirTransformer;
import de.gematik.test.erezept.fhir.builder.kbv.*;
import de.gematik.test.erezept.fhir.r4.kbv.*;
import de.gematik.test.erezept.fhir.values.AsvTeamNumber;
import de.gematik.test.erezept.fhir.values.BSNR;
import de.gematik.test.erezept.fhir.values.BaseANR.ANRType;
import de.gematik.test.erezept.fhir.values.KZVA;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public abstract class KbvBaseBundleInputTransformer<
        V extends ProfileVersion,
        R extends KbvBaseBundle,
        B extends KbvBaseDocumentBundleBuilder<V, R, B>>
    implements JsonToFhirTransformer<KbvEPrescriptionIn, R, B> {

  protected final HermesService hermes;

  protected final KbvPractitionerBuilder toFhirPractitioner(@Nullable KbvEPractitionerIn data) {
    data = Objects.requireNonNullElseGet(data, KbvEPractitionerIn::new);
    return hermes.toFhirBuilder(data, KbvPractitioner.class, KbvPractitionerBuilder.class);
  }

  protected final Optional<KbvPractitionerRoleBuilder> toFhirPractitionerRole(
      @Nullable KbvEPractitionerRoleIn data, KbvPractitioner practitioner) {
    return Optional.ofNullable(data)
        .map(
            pr -> {
              val practitionerRoleBuilder = KbvPractitionerRoleBuilder.builder();
              practitionerRoleBuilder.practitioner(practitioner);
              val asvNumber =
                  Optional.ofNullable(pr.getAsvnr())
                      .map(AsvTeamNumber::from)
                      .orElseGet(AsvTeamNumber::random);
              return practitionerRoleBuilder.teamNumber(asvNumber);
            });
  }

  protected final KbvPatientBuilder toFhirPatient(@Nullable KbvEPatientIn data) {
    data =
        Objects.requireNonNullElseGet(data, () -> KbvEPatientIn.withKvnr(KVNR.randomStringValue()));
    return hermes.toFhirBuilder(data, KbvPatient.class, KbvPatientBuilder.class);
  }

  protected final KbvCoverageBuilder toFhirCoverage(@Nullable KbvECoverageIn data) {
    data = Objects.requireNonNullElseGet(data, KbvECoverageIn::new);
    return hermes.toFhirBuilder(data, KbvCoverage.class, KbvCoverageBuilder.class);
  }

  protected final KbvMedicalOrganizationBuilder toFhirMedicalOrganization(
      @Nullable KbvEOrganizationIn dataIn, KbvPractitioner practitioner) {
    val data = Objects.requireNonNullElseGet(dataIn, KbvEOrganizationIn::new);
    val builder =
        hermes.toFhirBuilder(
            data, KbvMedicalOrganization.class, KbvMedicalOrganizationBuilder.class);

    practitioner
        .getANR()
        .filter(anr -> anr.getType().equals(ANRType.ZANR))
        .ifPresent(
            zanr -> {
              // if practitioner is a dentist (has ZANR) the organization requires a KZVA instead of
              // BSNR
              val kzvNumber = ofNullable(data.getBsnr()).orElseGet(() -> KZVA.random().getValue());
              builder
                  .kzva(KZVA.from(kzvNumber))
                  // TODO: KbvMedicalOrganizationInputTransformer is not capable of checking this by
                  // its own
                  .bsnr((BSNR) null);
            });

    return builder;
  }

  protected B initializeCommonBuilder(B builder, String prescriptionId, KbvEPrescriptionIn data) {
    return builder.prescriptionId(PrescriptionId.from(prescriptionId));
  }

  @Override
  public final Class<KbvEPrescriptionIn> jsonType() {
    return KbvEPrescriptionIn.class;
  }
}
