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

package de.gematik.erpione.server.usecase.raw;

import static java.util.Optional.ofNullable;

import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.erpione.server.usecase.ErpioneActorRawFhirResponse;
import de.gematik.test.erezept.client.usecases.TaskGetByIdCommand;
import de.gematik.test.erezept.fhir.r4.erp.ErxPrescriptionBundle;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.values.Secret;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ReFetchReceipt
    implements ErpioneActorRawFhirResponse<HealthcareInstitution, ErxPrescriptionBundle> {

  private final PrescriptionId pid;
  @Nullable private final Secret secret;

  /**
   * Executes the use case with the given actor and return the raw FHIR resource.
   *
   * @param actor
   * @return a result of type R, which is the raw FHIR output of the use case
   */
  @Override
  public ErxPrescriptionBundle performAs(HealthcareInstitution actor) {
    // if no secret was given look into DB and try to recover with prescription ID
    val s =
        ofNullable(secret)
            .orElseGet(
                () ->
                    actor
                        .getRepository()
                        .getPrescriptionById(pid.getValue())
                        .map(p -> Secret.from(p.getSecret()))
                        // secret is nullable but the FD simply won't allow a pharmacy to fetch the
                        // receipt without a secret
                        .orElse(null));
    val cmd = new TaskGetByIdCommand(pid.toTaskId(), s);
    return actor.requestAndUnpack(pid, cmd);
  }

  public static ReFetchReceipt with(String prescriptionId, @Nullable String secret) {
    val pid = PrescriptionId.from(prescriptionId);
    val s = ofNullable(secret).map(Secret::from).orElse(null);
    return new ReFetchReceipt(pid, s);
  }
}
