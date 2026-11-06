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

package de.gematik.erpione.server.usecase;

import static java.util.Optional.ofNullable;

import com.ibm.icu.impl.Pair;
import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.MedicationEBatchIn;
import de.gematik.erpione.data.r4.eu.EuPrescriptionDispensationBatchOut;
import de.gematik.erpione.data.r4.eu.EuPrescriptionDispensationIn;
import de.gematik.erpione.data.r4.eu.EuPrescriptionDispensationListIn;
import de.gematik.erpione.data.r4.eu.EuPrescriptionDispensationOut;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.test.erezept.client.usecases.eu.EuCloseInputPostCommand;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.eu.*;
import de.gematik.test.erezept.fhir.r4.eu.EuHealthcareFacilityType;
import de.gematik.test.erezept.fhir.r4.eu.EuMedication;
import de.gematik.test.erezept.fhir.r4.eu.EuMedicationDispense;
import de.gematik.test.erezept.fhir.r4.eu.EuOrganizationProfession;
import de.gematik.test.erezept.fhir.values.EuAccessCode;
import de.gematik.test.erezept.fhir.valuesets.eu.EuRequestType;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hl7.fhir.r4.model.Identifier;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CloseEuPharmaceuticals
    implements ErpioneActorUsecase<HealthcareInstitution, EuPrescriptionDispensationBatchOut> {

  private final KVNR kvnr;
  private final EuAccessCode accessCode;
  private final List<EuPrescriptionDispensationIn> dispensations;

  /**
   * Executes the use case with the given actor and data.
   *
   * @param actor to perform this use case
   * @return a result of type D, which is the output of the use case
   */
  @Override
  public EuPrescriptionDispensationBatchOut performAs(HealthcareInstitution actor) {
    val orgName = actor.getInfo().getName();
    val practitionerName = GemFaker.fakerName();
    val practitionerIdentifier =
        new Identifier().setSystem("http://system").setValue("value-12345");

    val practitionerRole = EuPractitionerRoleBuilder.getSimplePractitionerRole();
    val euPractitioner =
        EuPractitionerBuilder.buildPractitioner().identifier(practitionerIdentifier).build();
    // Note: non-national pharmacies have their own identification systems which we cannot know here
    val euPharmacy =
        EuOrganizationFaker.faker()
            .withNcpehCountry(actor.getCountryCode())
            .withName(orgName)
            .fake();

    val prescriptionInput =
        EuGetPrescriptionInputBuilder.forRequestType(EuRequestType.PRESCRIPTION_LIST)
            .kvnr(kvnr)
            .accessCode(accessCode)
            .countryCode(actor.getCountryCode())
            .practitionerRole(EuOrganizationProfession.getDefaultPharmacist())
            .pointOfCare(orgName)
            .healthcareFacilityType(EuHealthcareFacilityType.getDefault())
            .practitionerName(practitionerName)
            .build();

    val repo = actor.getRepository();
    val euDispenseResponses =
        dispensations.stream()
            .map(dispensationIn -> this.createEuMedicationDispense(actor, dispensationIn))
            .map(
                pair -> {
                  val medication = pair.first;
                  val md = pair.second;
                  val prescriptionId = md.getPrescriptionId().getValue();

                  val euCloseOperationInput =
                      EuCloseOperationInputBuilder.builder()
                          .requestDataFromGetPrescriptionInput(prescriptionInput)
                          .practitioner(euPractitioner)
                          .organization(euPharmacy)
                          .practitionerRole(practitionerRole)
                          .withOptionalRxDispensation(md, medication)
                          .build();

                  val cmd = new EuCloseInputPostCommand(euCloseOperationInput);
                  val response = actor.request(cmd);

                  if (response.getStatusCode() < 300) {
                    /* TODO: why not setting the status to completed in the local repository?
                       This would be useful for tracking which prescriptions have been dispensed.

                       For now after successful dispensation, remove the prescriptions from local repo
                    */
                    repo.deletePrescription(prescriptionId);
                  }

                  /*
                  TODO: implement transformer for EuCloseOperationInput + EuCloseEOperationOut
                  TODO: implement transformer for OperationOutcome (to ErrorData)
                  then we can return a more detailed response with the status of the dispensation, any errors, etc.
                   */
                  return new EuPrescriptionDispensationOut(
                      prescriptionId, response.getStatusCode());
                })
            .toList();

    return new EuPrescriptionDispensationBatchOut(euDispenseResponses);
  }

  /**
   * Extract the Medication Batch from given input, or create a new one with random values if not
   * present.
   *
   * @param batchIn given by the user, can be null
   * @return a MedicationEBatchIn object, either from input or newly created
   */
  private MedicationEBatchIn extractMedicationBatch(@Nullable MedicationEBatchIn batchIn) {
    return ofNullable(batchIn)
        .orElseGet(
            () ->
                new MedicationEBatchIn(
                    GemFaker.fakerLotNumber(), GemFaker.fakerFutureExpirationDate()));
  }

  /**
   * Create a EuMedication and EuMedicationDispense pair from the given input.
   *
   * @param actor the healthcare institution performing the dispensation
   * @param dispensationIn the input data for the dispensation
   * @return a Pair containing the EuMedication and EuMedicationDispense objects
   */
  private Pair<EuMedication, EuMedicationDispense> createEuMedicationDispense(
      HealthcareInstitution actor, EuPrescriptionDispensationIn dispensationIn) {
    val performerId = actor.getId();
    val batch = this.extractMedicationBatch(dispensationIn.getBatch());
    val lotNumber = ofNullable(batch.getLotNumber()).orElseGet(GemFaker::fakerLotNumber);
    val expiryDate =
        ofNullable(batch.getExpiryDate()).orElseGet(GemFaker::fakerFutureExpirationDate);

    val wasSubstituted =
        ofNullable(dispensationIn.getWasSubstituted())
            .orElseGet(() -> dispensationIn.getMedication() == null);
    val medIn = ofNullable(dispensationIn.getMedication()).orElseGet(EMedication::new);
    val medication = actor.getHermes().toFhirResource(EuMedication.class, medIn);

    val md =
        EuMedicationDispenseBuilder.forKvnr(kvnr)
            .performerId(performerId)
            .prescriptionId(dispensationIn.getPrescriptionId())
            .medication(medication)
            .whenPrepared(new Date())
            .whenHandedOver(new Date())
            .batch(lotNumber, expiryDate)
            .wasSubstituted(wasSubstituted)
            .build();

    return Pair.of(medication, md);
  }

  public static CloseEuPharmaceuticals using(
      String kvnr, String accessCode, EuPrescriptionDispensationListIn body) {

    return new CloseEuPharmaceuticals(
        KVNR.from(kvnr), EuAccessCode.from(accessCode), body.getPrescriptions());
  }
}
