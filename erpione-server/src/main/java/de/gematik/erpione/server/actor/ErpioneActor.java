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

package de.gematik.erpione.server.actor;

import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.erpione.data.actor.ErpioneInstitutionData;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.error.ErpResponseExpectationError;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.erpione.server.util.ErpioneLocalVerifier;
import de.gematik.test.erezept.client.usecases.ErpBaseCommand;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import java.util.List;
import lombok.val;
import org.hl7.fhir.r4.model.Resource;

/** ErpioneActor is the common base of a stakeholder in the healthcare system. */
public interface ErpioneActor {

  void initialize();

  ErpioneInstitutionData getInfo();

  default String getId() {
    return this.getInfo().getMnemonicId();
  }

  default String getMnemonic() {
    return this.getInfo().getMnemonic();
  }

  default String getName() {
    return this.getInfo().getName();
  }

  HermesService getHermes();

  ErpionePersistence getRepository();

  /**
   * Perform a request against the E-Rezept Fachdienst
   *
   * @param request
   * @param <R>
   * @return
   */
  <R extends Resource> FhirBResponse<R> request(ErpBaseCommand<R> request);

  /**
   * Performs a request against the E-Rezept Fachdienst and unpacks the response. If the response is
   * not of the expected type, e.g., is an OperationOutcome, an ErpResponseExpectationException is
   * thrown.
   *
   * @param request to be performed
   * @return only positive response bodies of type expected type
   * @param <R> type expected on positive response
   */
  default <R extends Resource> R requestAndUnpack(ErpBaseCommand<R> request) {
    val response = this.request(request);
    return response.getExpectedOrThrow(
        it -> new ErpResponseExpectationError(this.getMnemonic(), it));
  }

  /**
   * Performs a request against the E-Rezept Fachdienst and unpacks the response. If the response is
   * not of the expected type, e.g., is an OperationOutcome, an ErpResponseExpectationException is
   * thrown.
   *
   * <p>If the response code is 410 (GONE), the prescription is deleted from local database
   *
   * @param pid to which this request is virtually bound
   * @param request to be performed
   * @return only positive response bodies of type expected type
   * @param <R> type expected on positive response
   */
  default <R extends Resource> R requestAndUnpack(PrescriptionId pid, ErpBaseCommand<R> request) {
    val response = this.request(request);
    val status = response.getStatusCode();
    val isGone = List.of(404, 410).contains(status);
    if (isGone) {
      // make sure the local database is cleaned up when encountering prescriptions
      // which are already gone on the Fachdienst
      this.getRepository().deletePrescription(pid.getValue());
    }
    return response.getExpectedOrThrow(
        it -> new ErpResponseExpectationError(this.getMnemonic(), it));
  }

  /**
   * decode a signed FHIR document into a specific FHIR resource class.
   *
   * @param expectedClass
   * @param signedDocument
   * @param <T>
   * @return
   */
  default <T extends Resource> T decode(Class<T> expectedClass, byte[] signedDocument) {
    val document = ErpioneLocalVerifier.parse(signedDocument).getDocument();
    return decode(expectedClass, document);
  }

  default <T extends Resource> T decode(Class<T> expectedClass, String content) {
    return this.getHermes().decode(expectedClass, content);
  }
}
