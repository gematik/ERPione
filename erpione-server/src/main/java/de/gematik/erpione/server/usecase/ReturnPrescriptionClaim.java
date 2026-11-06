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

import de.gematik.erpione.data.error.EmptyData;
import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.test.erezept.client.usecases.TaskRejectCommand;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.values.Secret;
import lombok.val;

/** Use case $reject on a prescription claim */
public class ReturnPrescriptionClaim
    implements ErpioneActorUsecase<HealthcareInstitution, EmptyData> {

  private final PrescriptionId prescriptionId;

  private ReturnPrescriptionClaim(PrescriptionId prescriptionId) {
    this.prescriptionId = prescriptionId;
  }

  /**
   * Executes the use case with the given actor and data.
   *
   * @return a result of type D, which is the output of the use case
   */
  @Override
  public EmptyData performAs(HealthcareInstitution actor) {
    val task = actor.getRepository().getPrescriptionByIdOrThrow(prescriptionId.getValue());

    val request =
        new TaskRejectCommand(
            prescriptionId.toTaskId(),
            AccessCode.from(task.getAccessCode()),
            Secret.from(task.getSecret()));

    actor.requestAndUnpack(prescriptionId, request);

    // update the state manually because we don't have any further information from FD
    val repo = actor.getRepository();
    repo.getPrescriptionById(prescriptionId.getValue())
        .ifPresent(
            te -> {
              te.setStatus(PrescriptionEStatus.READY);
              te.setSecret(null);
              repo.save(te);
            });

    return new EmptyData();
  }

  public static ReturnPrescriptionClaim forPrescription(String prescriptionId) {
    return new ReturnPrescriptionClaim(PrescriptionId.from(prescriptionId));
  }
}
