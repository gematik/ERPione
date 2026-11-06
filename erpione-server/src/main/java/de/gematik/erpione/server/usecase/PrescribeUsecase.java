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

import de.gematik.bbriccs.fhir.de.valueset.InsuranceTypeDe;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionOut;
import de.gematik.erpione.server.actor.HealthcareProvider;
import de.gematik.test.erezept.client.usecases.TaskActivateCommand;
import de.gematik.test.erezept.client.usecases.TaskCreateCommand;
import de.gematik.test.erezept.fhir.r4.erp.ErxTask;
import de.gematik.test.erezept.fhir.r4.kbv.KbvBaseBundle;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import de.gematik.test.erezept.fhir.r4.kbv.KbvEvdgaBundle;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.MedicationCategory;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class PrescribeUsecase<B extends KbvBaseBundle>
    implements ErpioneActorUsecase<HealthcareProvider, ErpETask> {

  private final B prescriptionBundle;
  private final PrescriptionFlowType flowType;

  private PrescribeUsecase(B prescriptionBundle, PrescriptionFlowType flowType) {
    this.prescriptionBundle = prescriptionBundle;
    this.flowType = flowType;
  }

  @Override
  public final ErpETask performAs(HealthcareProvider actor) {
    val draftTask = createTask(actor);
    val taskData = activateTask(draftTask, actor);

    return actor.getRepository().save(taskData);
  }

  private ErxTask createTask(HealthcareProvider actor) {
    val request = new TaskCreateCommand(flowType);
    return actor.requestAndUnpack(request);
  }

  private ErpETask activateTask(ErxTask draftTask, HealthcareProvider actor) {
    val prescriptionId = draftTask.getPrescriptionId().toTaskId();
    val accessCode = draftTask.getAccessCode();

    // adjust the created bundle
    adjustBundle(draftTask, prescriptionBundle);

    val signedPair = actor.signDocument(prescriptionBundle);
    val signed = signedPair.first;
    val signature = signedPair.second;

    val request = new TaskActivateCommand(prescriptionId, accessCode, signed);
    val erxTask = actor.requestAndUnpack(request);

    val taskData = actor.getHermes().toJson(ErpETask.class, erxTask);
    val prescriptionData = actor.getHermes().toJson(KbvEPrescriptionOut.class, prescriptionBundle);
    taskData.setPrescription(prescriptionData);
    taskData.setPrescriptionReference(prescriptionBundle.getLogicalId());
    taskData.setSignature(signature);

    return taskData;
  }

  private void adjustBundle(ErxTask from, B to) {
    // make sure we get the value of the prescription ID but still remain the system from the
    // original kbv bundle because otherwise we will have a version mixture
    val draftPrescriptionId = from.getPrescriptionId();
    val kbvBundlePrescriptionIdentifier = to.getPrescriptionId().asIdentifier();
    kbvBundlePrescriptionIdentifier.setValue(draftPrescriptionId.getValue());
    val prescriptionId = PrescriptionId.from(kbvBundlePrescriptionIdentifier);

    to.setPrescriptionId(prescriptionId);
    to.setId(UUID.randomUUID().toString());

    // will update all dates contained within the KBV Bundle to the current date
    to.setAllDates();
    if (to instanceof KbvErpBundle kbvErp && kbvErp.getMedicationRequest().isMultiple()) {
      kbvErp.getMedicationRequest().updateMvoDates();
    }

    /*
    While lastUpdated is optional for most WFs, it is not permitted to set lastUpdated
    for the WF 162 (more precisely the KBV_PR_EVDGA_Bundle).
    */
    to.getMeta().setLastUpdated(null);
  }

  public static PrescribeUsecase<KbvEvdgaBundle> forDiGA(KbvEvdgaBundle bundle) {
    return new PrescribeUsecase<>(bundle, PrescriptionFlowType.FLOW_TYPE_162);
  }

  public static PrescribeUsecase<KbvErpBundle> forPharmaceutical(
      KbvErpBundle bundle, boolean asDirectAssignment) {

    val isTRezept = MedicationCategory.C_02 == bundle.getMedication().getCategoryFirstRep();

    val insuranceType = bundle.getCoverage().getInsuranceKindOptional().orElse(InsuranceTypeDe.GKV);
    val flowType =
        isTRezept
            ? PrescriptionFlowType.FLOW_TYPE_166
            : PrescriptionFlowType.fromInsuranceKind(insuranceType, asDirectAssignment);

    return new PrescribeUsecase<>(bundle, flowType);
  }
}
