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

import de.gematik.erpione.data.r4.kbv.KbvEHealthAppRequestIn;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionIn;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.impl.HermesUtils;
import de.gematik.test.erezept.fhir.builder.kbv.KbvEvdgaBundleBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvHealthAppRequestBuilder;
import de.gematik.test.erezept.fhir.profiles.version.ErpWorkflowVersion;
import de.gematik.test.erezept.fhir.profiles.version.KbvItaForVersion;
import de.gematik.test.erezept.fhir.profiles.version.KbvItvEvdgaVersion;
import de.gematik.test.erezept.fhir.r4.kbv.KbvEvdgaBundle;
import de.gematik.test.erezept.fhir.values.AsvFachgruppennummer;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import de.gematik.test.erezept.fhir.valuesets.StatusKennzeichen;
import java.util.Objects;
import java.util.Optional;
import lombok.val;
import org.hl7.fhir.r4.model.DeviceRequest;

public class KbvEvdgaBundleInputTransformer
    extends KbvBaseBundleInputTransformer<
        KbvItvEvdgaVersion, KbvEvdgaBundle, KbvEvdgaBundleBuilder> {

  public KbvEvdgaBundleInputTransformer(HermesService hermes) {
    super(hermes);
  }

  /**
   * Note: this is a workaround because in ErpWorkflow 1.6 we have to choose a NOT the default
   * ErpItaForVersion
   *
   * @return
   */
  private KbvItaForVersion getKbvItaForVersion() {
    if (ErpWorkflowVersion.getDefaultVersion().isEqual(ErpWorkflowVersion.V1_6)) {
      return KbvItaForVersion.V1_2_0;
    } else {
      return KbvItaForVersion.getDefaultVersion();
    }
  }

  @Override
  public KbvEvdgaBundleBuilder toFhir(KbvEPrescriptionIn data) {
    val itaForVersion = getKbvItaForVersion();

    val hardata =
        Objects.requireNonNullElseGet(data.getHealthAppRequest(), KbvEHealthAppRequestIn::new);
    val hatPzn = HermesUtils.requireNonNullElseGet(hardata.getPzn(), () -> "19205615");
    val harName =
        HermesUtils.requireNonNullElseGet(
            hardata.getName(), () -> "Vantis KHK und Herzinfarkt 001");

    val patient = this.toFhirPatient(data.getPatient()).version(itaForVersion).build();
    val coverage = this.toFhirCoverage(data.getCoverage()).version(itaForVersion).build();

    val practitionerBuilder =
        this.toFhirPractitioner(data.getPractitioner()).version(itaForVersion);
    Optional.ofNullable(data.getPractitionerRole())
        .flatMap(it -> Optional.ofNullable(it.getAsvnr()))
        .ifPresent(
            asvnr -> {
              practitionerBuilder.anr(null); // ANR and ASV are mutually exclusive
              practitionerBuilder.addQualification(AsvFachgruppennummer.from(asvnr));
            });
    val practitioner = practitionerBuilder.build();

    val medOrg =
        this.toFhirMedicalOrganization(data.getOrganization(), practitioner)
            .version(itaForVersion)
            .build();

    val har =
        KbvHealthAppRequestBuilder.forPatient(patient)
            .insurance(coverage)
            .requester(practitioner)
            .status(DeviceRequest.DeviceRequestStatus.ACTIVE)
            .intent(DeviceRequest.RequestIntent.ORDER)
            .relatesToSocialCompensationLaw(hardata.getSer())
            .healthApp(hatPzn, harName)
            .build();

    val flowType = PrescriptionFlowType.FLOW_TYPE_162;
    val prescriptionId = PrescriptionId.random(flowType);

    val builder =
        this.initializeCommonBuilder(
            KbvEvdgaBundleBuilder.forPrescription(prescriptionId), prescriptionId.getValue(), data);

    this.toFhirPractitionerRole(data.getPractitionerRole(), practitioner)
        .map(
            prb -> {
              prb.version(itaForVersion);
              return prb.build();
            })
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
        .healthAppRequest(har);
  }

  @Override
  public Class<KbvEvdgaBundle> fhirType() {
    return KbvEvdgaBundle.class;
  }
}
