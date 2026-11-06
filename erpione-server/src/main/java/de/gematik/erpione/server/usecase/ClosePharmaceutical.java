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

import static java.text.MessageFormat.format;
import static org.hl7.fhir.r4.model.MedicationDispense.MedicationDispenseStatus.COMPLETED;

import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.data.r4.gem.ErpEReceiptOut;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.test.erezept.client.usecases.CloseTaskCommand;
import de.gematik.test.erezept.client.usecases.TaskGetByIdCommand;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.erp.ErxMedicationDispenseBuilder;
import de.gematik.test.erezept.fhir.builder.erp.GemOperationInputParameterBuilder;
import de.gematik.test.erezept.fhir.r4.erp.GemCloseOperationParameters;
import de.gematik.test.erezept.fhir.r4.erp.GemErpMedication;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.values.Secret;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ClosePharmaceutical
    implements ErpioneActorUsecase<HealthcareInstitution, ErpEReceiptOut> {

  private final PrescriptionId prescriptionId;
  private final @Nullable GemCloseOperationParameters params;

  /**
   * Executes the use case with the given actor and data.
   *
   * @param actor
   * @return a result of type D, which is the output of the use case
   */
  @Override
  public ErpEReceiptOut performAs(HealthcareInstitution actor) {
    val prescriptionTask =
        actor.getRepository().getPrescriptionByIdOrThrow(prescriptionId.getValue());
    val prescription = prescriptionTask.getPrescription();
    val secret = Secret.from(prescriptionTask.getSecret());

    CloseTaskCommand cmd;

    if (this.params == null) {
      // no dispense body received from controller: check if $dispense was already performed
      cmd = withoutCustomDispenseBody(actor, prescriptionTask);
    } else {
      // we need to adjust some data
      params.getParameters("rxDispensation").stream()
          .flatMap(
              it -> it.getPart().stream().filter(p -> "medicationDispense".equals(p.getName())))
          .map(it -> (MedicationDispense) it.getResource())
          .forEach(
              it -> {
                it.getIdentifierFirstRep().setValue(prescription.getPrescriptionId());
                it.getSubject().getIdentifier().setValue(prescription.getPatient().getKvnr());
                it.getPerformerFirstRep()
                    .getActor()
                    .getIdentifier()
                    .setValue(actor.getTelematikId());
              });

      cmd = new CloseTaskCommand(prescriptionId.toTaskId(), secret, this.params);
    }

    val receipt = actor.requestAndUnpack(cmd);
    val receiptData = actor.getHermes().toJson(ErpEReceiptOut.class, receipt);

    val repo = actor.getRepository();
    repo.save(receiptData);
    repo.getPrescriptionById(prescriptionId.getValue())
        .ifPresent(
            te -> {
              te.setStatus(PrescriptionEStatus.COMPLETED);
              te.setLastMedicationDispense(new Date());
              te.setReceiptReference(receipt.getId());
              te.setPrescriptionReference(
                  te.getPrescriptionReference()); // TODO: why is this getting lost otherwise?
              repo.save(te);
            });

    return receiptData;
  }

  private CloseTaskCommand withoutCustomDispenseBody(
      HealthcareInstitution actor, ErpETask prescriptionTask) {
    // fetch the task from FD to ensure that potential $dispenses are not overwritten by the close
    // operation
    val secret = Secret.from(prescriptionTask.getSecret());
    val prescriptionBundle =
        actor.requestAndUnpack(
            prescriptionId, new TaskGetByIdCommand(prescriptionId.toTaskId(), secret));
    val hasMedicationDispense = prescriptionBundle.getTask().hasLastMedicationDispenseDate();

    if (hasMedicationDispense) {
      return new CloseTaskCommand(prescriptionId.toTaskId(), secret);
    } else {
      // not $dispense was performed yet, extract MedicationDispense information from original
      // prescription
      val ePrescription = Objects.requireNonNull(prescriptionTask.getPrescription());
      val eMedication =
          Objects.requireNonNull(
              ePrescription.getMedication(),
              format("Prescription {0} does not have a medication", prescriptionId.getValue()));
      val kvnr = ePrescription.getPatient().getKvnr();
      val eMedicationRequest =
          Objects.requireNonNull(
              ePrescription.getMedicationRequest(),
              format(
                  "Prescription {0} does not have a medication request",
                  prescriptionId.getValue()));

      val medication = actor.getHermes().toFhirResource(GemErpMedication.class, eMedication);

      // TODO: could hermes handle this as well?
      val mdb =
          ErxMedicationDispenseBuilder.forKvnr(KVNR.from(kvnr))
              .performerId(actor.getTelematikId())
              .prescriptionId(prescriptionId)
              .medication(medication)
              .status(COMPLETED)
              .whenPrepared(new Date())
              .whenHandedOver(new Date())
              .batch(GemFaker.fakerLotNumber(), GemFaker.fakerFutureExpirationDate())
              .wasSubstituted(false);

      Optional.ofNullable(eMedicationRequest.getDosage()).ifPresent(mdb::dosageInstruction);
      Optional.ofNullable(eMedicationRequest.getNote()).ifPresent(mdb::note);

      val closeParams =
          GemOperationInputParameterBuilder.forClosingPharmaceuticals()
              .with(mdb.build(), medication)
              .build();
      return new CloseTaskCommand(prescriptionId.toTaskId(), secret, closeParams);
    }
  }

  public static Builder with(String prescriptionId) {
    return new Builder(PrescriptionId.from(prescriptionId));
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {
    private final PrescriptionId prescriptionId;

    public ClosePharmaceutical using(@Nullable GemCloseOperationParameters params) {
      return new ClosePharmaceutical(prescriptionId, params);
    }
  }
}
