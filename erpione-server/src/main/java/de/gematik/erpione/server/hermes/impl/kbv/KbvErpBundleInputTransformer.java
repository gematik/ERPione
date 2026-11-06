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

import de.gematik.bbriccs.fhir.de.valueset.InsuranceTypeDe;
import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.kbv.KbvEMedicationRequestIn;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionIn;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.kbv.KbvErpBundleBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvErpMedicationRequestBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvPractitionerRoleBuilder;
import de.gematik.test.erezept.fhir.profiles.version.KbvItaErpVersion;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpMedication;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpMedicationRequest;
import de.gematik.test.erezept.fhir.values.AsvFachgruppennummer;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.MedicationCategory;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import de.gematik.test.erezept.fhir.valuesets.StatusKennzeichen;
import java.util.Objects;
import lombok.val;

public class KbvErpBundleInputTransformer
    extends KbvBaseBundleInputTransformer<KbvItaErpVersion, KbvErpBundle, KbvErpBundleBuilder> {

  public KbvErpBundleInputTransformer(HermesService hermes) {
    super(hermes);
  }

  @Override
  public Class<KbvErpBundle> fhirType() {
    return KbvErpBundle.class;
  }

  @Override
  public KbvErpBundleBuilder toFhir(KbvEPrescriptionIn data) {
    val mrd =
        Objects.requireNonNullElseGet(data.getMedicationRequest(), KbvEMedicationRequestIn::new);
    val mrb =
        hermes.toFhirBuilder(
            mrd, KbvErpMedicationRequest.class, KbvErpMedicationRequestBuilder.class);

    val med = Objects.requireNonNullElseGet(data.getMedication(), EMedication::new);
    val mb = hermes.toFhirBuilder(med, KbvErpMedication.class, HermesKbvMedicationBuilder.class);

    val patient = this.toFhirPatient(data.getPatient()).build();
    val coverage = this.toFhirCoverage(data.getCoverage()).build();
    val medication = mb.build();

    val practitionerBuilder = this.toFhirPractitioner(data.getPractitioner());
    ofNullable(data.getPractitionerRole())
        .flatMap(it -> ofNullable(it.getAsvnr()))
        .ifPresent(
            asvnr -> {
              practitionerBuilder.anr(null); // ANR and ASV are mutually exclusive
              practitionerBuilder.addQualification(AsvFachgruppennummer.from(asvnr));
            });
    val practitioner = practitionerBuilder.build();

    val medOrgBuilder = this.toFhirMedicalOrganization(data.getOrganization(), practitioner);
    val medOrg = medOrgBuilder.build();

    mrb.insurance(coverage).medication(medication).requester(practitioner);

    val isTRezept = MedicationCategory.C_02 == medication.getCategoryFirstRep();
    if (isTRezept) {
      mrb.expectedSupplyDurationInWeeks(GemFaker.fakerAmount(1, 12));
    }
    val medicationRequest = mrb.build();

    val insuranceType = coverage.getInsuranceKindOptional().orElse(InsuranceTypeDe.GKV);
    val flowType =
        isTRezept
            ? PrescriptionFlowType.FLOW_TYPE_166
            : PrescriptionFlowType.fromInsuranceKind(insuranceType, false);
    val prescriptionId =
        PrescriptionId.random(flowType).getValue(); // by default, will be replaced before QES

    val builder = this.initializeCommonBuilder(KbvErpBundleBuilder.builder(), prescriptionId, data);

    this.toFhirPractitionerRole(data.getPractitionerRole(), practitioner)
        .map(KbvPractitionerRoleBuilder::build)
        .ifPresent(
            pr -> {
              builder.statusKennzeichen(StatusKennzeichen.ASV, practitioner);
              builder.practitionerRole(pr);
            });

    return builder
        .patient(patient)
        .insurance(coverage)
        .practitioner(practitioner)
        .medicalOrganization(medOrg)
        .medicationRequest(medicationRequest)
        .medication(medication);
  }
}
