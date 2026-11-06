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

import de.gematik.erpione.data.r4.gem.ErpESignature;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionOut;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.test.erezept.client.usecases.TaskAcceptCommand;
import de.gematik.test.erezept.fhir.r4.erp.ErxAcceptBundle;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import de.gematik.test.erezept.fhir.r4.kbv.KbvEvdgaBundle;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import java.util.Date;
import java.util.Optional;
import lombok.val;
import org.bouncycastle.util.encoders.Base64;

/** Use case $accept on a prescription */
public class ClaimPrescription implements ErpioneActorUsecase<HealthcareInstitution, ErpETask> {

  private final PrescriptionId prescriptionId;
  private final AccessCode accessCode;

  private ClaimPrescription(PrescriptionId pid, AccessCode ac) {
    this.prescriptionId = pid;
    this.accessCode = ac;
  }

  public static ClaimPrescription with(String prescriptionId, String accessCode) {
    return new ClaimPrescription(PrescriptionId.from(prescriptionId), AccessCode.from(accessCode));
  }

  /**
   * Executes the use case with the given actor and data.
   *
   * @return a result of type D, which is the output of the use case
   */
  @Override
  public ErpETask performAs(HealthcareInstitution actor) {
    val acceptCommand = new TaskAcceptCommand(prescriptionId.toTaskId(), accessCode);
    val acceptBundle = actor.requestAndUnpack(prescriptionId, acceptCommand);

    val task = acceptBundle.getTask();
    val taskData = actor.getHermes().toJson(ErpETask.class, task);

    // update the prescription information
    // even if the prescription info was already know, just override!
    val prescription = extractPrescriptionData(actor, acceptBundle);
    taskData.setPrescription(prescription);

    // we might already have some information about the signature, sometimes not
    val knownSig =
        actor
            .getRepository()
            .getPrescriptionById(prescriptionId.getValue())
            .flatMap(it -> Optional.ofNullable(it.getSignature()));

    // extract signature information from AcceptBundle or take the know signature otherwise
    val signed =
        Optional.ofNullable(acceptBundle.getSignature().getWhen())
            .orElseGet(() -> knownSig.map(ErpESignature::getDate).orElseGet(() -> new Date(0)));

    val tid = knownSig.map(ErpESignature::getTelematikId).orElse("unknown");
    val signer = knownSig.map(ErpESignature::getSigner).orElse("unknown");
    val cardType = knownSig.map(ErpESignature::getCardType).orElse("unknown");

    val signedB64 = Base64.toBase64String(acceptBundle.getSignedKbvBundle());
    val signature = new ErpESignature(signed, tid, signer, cardType, null, null, signedB64);
    taskData.setSignature(signature);

    return actor.getRepository().save(taskData);
  }

  private KbvEPrescriptionOut extractPrescriptionData(
      HealthcareInstitution actor, ErxAcceptBundle acceptBundle) {
    val flowType = acceptBundle.getTaskId().getFlowType();

    val fhirType =
        flowType.equals(PrescriptionFlowType.FLOW_TYPE_162)
            ? KbvEvdgaBundle.class
            : KbvErpBundle.class;

    val prescriptionBundle = actor.decode(fhirType, acceptBundle.getSignedKbvBundle());
    return actor.getHermes().toJson(KbvEPrescriptionOut.class, prescriptionBundle);
  }
}
