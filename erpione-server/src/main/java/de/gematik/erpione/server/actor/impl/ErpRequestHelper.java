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

package de.gematik.erpione.server.actor.impl;

import static java.text.MessageFormat.format;

import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.erpione.server.actor.ErpioneActor;
import de.gematik.erpione.server.hermes.error.ErpResponseExpectationError;
import de.gematik.idp.client.IdpClientRuntimeException;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.usecases.ErpBaseCommand;
import lombok.val;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

public class ErpRequestHelper {

  /**
   * In certain occasions when e.g. the FD cannot create a VAU session we receive a non-FHIR
   * response which leads to confusing Erpione responses. This wrapper method handles these errors
   * and creates a clear error message.
   *
   * @param request to be performed safely
   * @return the response if successful, otherwise throws an ErpResponseExpectationError with a
   *     proper OperationOutcome
   * @param <R> is the type of the expected response resource
   */
  protected static <R extends Resource> FhirBResponse<R> requestSafely(
      ErpioneActor actor, ErpClient client, ErpBaseCommand<R> request) {
    try {
      return client.request(request);
    } catch (IdpClientRuntimeException e) {
      val details = "Unexpected IDP Runtime Error";
      val oo = createErrorOperationOutcome(details, e.getMessage());
      throw new ErpResponseExpectationError(
          actor.getMnemonic(), 503, request.expectedResponseType(), oo);
    } catch (Exception e) {
      val details =
          format(
              "Unexpected Error during Request to {0} {1}",
              request.getMethod(), request.getRequestLocator());

      // attention: this may leak sensitive information from the exception message!
      val oo = createErrorOperationOutcome(details, e.getMessage());
      throw new ErpResponseExpectationError(
          actor.getMnemonic(), 500, request.expectedResponseType(), oo);
    }
  }

  private static OperationOutcome createErrorOperationOutcome(
      String detailsText, String diagnostics) {
    val oo = new OperationOutcome();
    val issue = oo.getIssueFirstRep();
    issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
    issue.setDetails(new CodeableConcept().setText(detailsText));
    issue.setDiagnostics(diagnostics);
    return oo;
  }
}
