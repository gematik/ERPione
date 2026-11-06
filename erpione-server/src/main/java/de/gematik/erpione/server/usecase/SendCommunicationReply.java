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

import de.gematik.erpione.data.r4.gem.com.CommunicationEDataOut;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.erpione.server.error.ImpossibleOperationError;
import de.gematik.test.erezept.client.usecases.CommunicationPostCommand;
import de.gematik.test.erezept.fhir.r4.erp.ErxCommunication;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SendCommunicationReply
    implements ErpioneActorUsecase<HealthcareInstitution, CommunicationEDataOut> {

  private final ErxCommunication communication;

  /**
   * @param actor
   * @return
   */
  @Override
  public CommunicationEDataOut performAs(HealthcareInstitution actor) {
    val prescriptionId = communication.getBasedOnReferenceId();
    val task = actor.getRepository().getPrescriptionByIdOrThrow(prescriptionId.getValue());
    val prescription =
        Optional.ofNullable(task.getPrescription())
            .orElseThrow(
                () ->
                    new ImpossibleOperationError(
                        format(
                            "Prescription with ID {0} has no prescription data",
                            prescriptionId.getValue())));

    // adjust some of the values
    communication.getSender().getIdentifier().setValue(actor.getTelematikId());
    communication
        .getRecipientFirstRep()
        .getIdentifier()
        .setValue(prescription.getPatient().getKvnr());

    val response = actor.requestAndUnpack(new CommunicationPostCommand(communication));
    return actor.getHermes().toJson(CommunicationEDataOut.class, response);
  }

  public static SendCommunicationReply withCommunicationTemplate(ErxCommunication communication) {
    return new SendCommunicationReply(communication);
  }
}
