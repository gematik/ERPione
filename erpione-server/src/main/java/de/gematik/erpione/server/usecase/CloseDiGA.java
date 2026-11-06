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

import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.erpione.data.error.IssueData;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.data.r4.gem.ErpEReceiptOut;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionOut;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.erpione.server.error.ImpossibleOperationError;
import de.gematik.test.erezept.client.usecases.CloseTaskCommand;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.erp.ErxMedicationDispenseDiGAFaker;
import de.gematik.test.erezept.fhir.builder.erp.GemOperationInputParameterBuilder;
import de.gematik.test.erezept.fhir.extensions.erp.RedeemCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.values.Secret;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class CloseDiGA implements ErpioneActorUsecase<HealthcareInstitution, ErpEReceiptOut> {

  private final PrescriptionId prescriptionId;
  private final boolean approved;

  private KbvEPrescriptionOut checkPreconditions(ErpETask task) {
    val prescription = Objects.requireNonNull(task.getPrescription());

    val flowTypeCorrect = PrescriptionFlowType.FLOW_TYPE_162.equals(prescriptionId.getFlowType());
    val hasHar = Optional.ofNullable(prescription.getHealthAppRequest()).isPresent();
    val isCorrect = flowTypeCorrect && hasHar;
    if (!isCorrect) {
      val operation = approved ? "approve" : "decline";
      val title =
          format("Impossible to {0} prescription {1}", operation, prescriptionId.getValue());
      val issues = getIssueData(flowTypeCorrect, hasHar);

      throw new ImpossibleOperationError(title, issues);
    }

    return prescription;
  }

  private @NotNull ArrayList<IssueData> getIssueData(boolean flowTypeCorrect, boolean hasHar) {
    val issues = new ArrayList<IssueData>();

    if (!flowTypeCorrect) {
      val expected = PrescriptionFlowType.FLOW_TYPE_162;
      val actual = prescriptionId.getFlowType();

      val ftIssue = new IssueData();
      ftIssue.setSeverity("WARNING");
      ftIssue.setDetails(
          format(
              "Prescription Flowtype must be {0} ({1}) but was {2} ({3})",
              expected.getCode(), expected.getDisplay(), actual.getCode(), actual.getDisplay()));
      issues.add(ftIssue);
    }

    if (!hasHar) {
      val ftIssue = new IssueData();
      ftIssue.setSeverity("ERROR");
      ftIssue.setDetails("Missing Health App Request information");
      issues.add(ftIssue);
    }
    return issues;
  }

  @Override
  public ErpEReceiptOut performAs(HealthcareInstitution actor) {
    // dispense what was prescribed
    val task = actor.getRepository().getPrescriptionByIdOrThrow(prescriptionId.getValue());

    val prescription = checkPreconditions(task);
    val patient = prescription.getPatient();
    val kvnr = patient.getKvnr();
    val har = Optional.ofNullable(prescription.getHealthAppRequest()).orElseThrow();

    val secret = Secret.from(task.getSecret());

    val operationBuilder = GemOperationInputParameterBuilder.forClosingDiGA();
    val mdb =
        ErxMedicationDispenseDiGAFaker.builder()
            .withKvnr(KVNR.asGkv(kvnr))
            .withPrescriptionId(prescriptionId)
            .withPerformer(actor.getTelematikId())
            .withPzn(har.getPzn(), har.getName())
            .toBuilder();

    if (approved) {
      // approve (fake a random redeem code)
      mdb.redeemCode(RedeemCode.random());
    } else {
      // fake decline reason
      mdb.note(GemFaker.getFaker().backToTheFuture().quote());
    }

    val operationParams = operationBuilder.with(mdb.build()).build();
    val cmd = new CloseTaskCommand(prescriptionId.toTaskId(), secret, operationParams);
    val receipt = actor.requestAndUnpack(cmd);
    val receiptData = actor.getHermes().toJson(ErpEReceiptOut.class, receipt);

    val repo = actor.getRepository();
    repo.save(receiptData);
    repo.getPrescriptionById(prescriptionId.getValue())
        .ifPresent(
            te -> {
              te.setStatus(PrescriptionEStatus.COMPLETED);
              repo.save(te);
            });

    return receiptData;
  }

  public static CloseDiGA approve(String prescriptionId) {
    return new CloseDiGA(PrescriptionId.from(prescriptionId), true);
  }

  public static CloseDiGA decline(String prescriptionId) {
    return new CloseDiGA(PrescriptionId.from(prescriptionId), false);
  }
}
