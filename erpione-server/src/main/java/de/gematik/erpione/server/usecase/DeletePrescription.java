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

import de.gematik.erpione.data.error.EmptyData;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.erpione.server.error.ErpioneNotFoundError;
import de.gematik.test.erezept.client.usecases.TaskAbortCommand;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.values.Secret;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class DeletePrescription implements ErpioneActorUsecase<HealthcareInstitution, EmptyData> {

  private final PrescriptionId prescriptionId;
  @Nullable private final AccessCode accessCode;
  @Nullable private final Secret secret;

  public static Builder with(String prescriptionId) {
    return new Builder(PrescriptionId.from(prescriptionId));
  }

  /**
   * Executes the use case with the given actor and data.
   *
   * @param actor
   * @return a result of type D, which is the output of the use case
   */
  @Override
  public EmptyData performAs(HealthcareInstitution actor) {
    val task =
        actor
            .getRepository()
            .getPrescriptionById(prescriptionId.getValue())
            .orElseThrow(
                () ->
                    new ErpioneNotFoundError(
                        format(
                            "Prescription with id {0} not found in repository",
                            prescriptionId.getValue())));

    val ac = Optional.ofNullable(accessCode).orElseGet(() -> AccessCode.from(task.getAccessCode()));
    val sec =
        Optional.ofNullable(secret)
            .orElseGet(() -> Optional.ofNullable(task.getSecret()).map(Secret::from).orElse(null));

    val cmd = new TaskAbortCommand(prescriptionId.toTaskId(), ac, sec);
    actor.requestAndUnpack(prescriptionId, cmd);
    return actor.getRepository().deletePrescription(prescriptionId.getValue());
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {
    private final PrescriptionId prescriptionId;

    public DeletePrescription andAccess(
        @Nullable String accessCodeValue, @Nullable String secretValue) {
      val secret = Optional.ofNullable(secretValue).map(Secret::from).orElse(null);
      val accessCode = Optional.ofNullable(accessCodeValue).map(AccessCode::from).orElse(null);

      return new DeletePrescription(prescriptionId, accessCode, secret);
    }
  }
}
