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

package de.gematik.erpione.server.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import de.gematik.bbriccs.fhir.coding.exceptions.MissingFieldException;
import de.gematik.bbriccs.rest.HttpBResponse;
import de.gematik.bbriccs.rest.fd.FhirBResponseCreator;
import de.gematik.erpione.data.error.ErrorType;
import de.gematik.erpione.data.exception.InvalidCodeValueException;
import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.etype.InsurantEStatus;
import de.gematik.erpione.server.hermes.error.ErpResponseExpectationError;
import de.gematik.erpione.server.hermes.error.HermesTransformerNotFoundError;
import de.gematik.test.erezept.client.vau.VauException;
import de.gematik.test.erezept.fhir.parser.FhirParser;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpMedication;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ErpioneErrorHandlerTest {

  private final ErpioneErrorHandler handler = new ErpioneErrorHandler();

  @Test
  void shouldHandleActorNotFoundException() {
    val ex = new ErpioneNotFoundError("Actor Hades not found");
    val response = handler.handleActorNotFoundException(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }

  @Test
  void shouldHandleResourceNotFoundException() {
    val ex = new NoResourceFoundException(HttpMethod.GET, "http://localhost", "/some/random/path");
    val response = handler.handleResourceNotFoundException(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }

  @Test
  void handleSearchQueryExpectationError() {
    val cause = new NotImplementedException("some missing feature");
    val ex = new UnsupportedOperationException("Not yet supported", cause);
    val response = handler.handleSearchQueryExpectationError(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }

  @Test
  void handleImpossibleOperationError() {
    val ex = new ImpossibleOperationError("Operation is not possible");
    val response = handler.handleImpossibleOperationError(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }

  @Test
  void handleInvalidCodeValueException() {
    val ex = new InvalidCodeValueException(InsurantEStatus.class, "ABC");
    val response = handler.handleInvalidCodeValueError(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }

  @Test
  void handleRequestParserError() {
    val ex =
        new HttpMessageNotReadableException(
            "Operation is not possible",
            new HttpInputMessage() {
              /**
               * @return
               */
              @Override
              public HttpHeaders getHeaders() {
                return HttpHeaders.EMPTY;
              }

              /**
               * @return
               * @throws IOException
               */
              @Override
              public InputStream getBody() {
                return new ByteArrayInputStream("hello world".getBytes());
              }
            });
    val response = handler.handleRequestParserError(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }

  @Test
  void handleUncaughtExceptions() {
    val ex = new NullPointerException("Uncaught exception");
    val response = handler.handleUncaughtExceptions(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }

  @Test
  void handleVauConnectionError() {
    val ex = new VauException("Error while connecting to VAU");
    val response = handler.handleVauConnectionError(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }

  @Test
  void handleHermesError() {
    val ex = HermesTransformerNotFoundError.hermes2Fhir(EMedication.class, KbvErpMedication.class);
    val response = handler.handleHermesError(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }

  @Test
  void handleErpResponseExpectationError() {
    val fhirCodec = mock(FhirParser.class);
    val erpResponseFactory = new FhirBResponseCreator(fhirCodec, fhirCodec::decode);

    val erpResponse =
        erpResponseFactory
            .expecting(KbvErpMedication.class)
            .received(HttpBResponse.status(404).withPayload("some body"))
            .usedAccessToken("jwt")
            .withoutDuration();

    val ex = new ErpResponseExpectationError("Hades", erpResponse);
    val response = handler.handleErpResponseExpectationError(ex);
    val body = response.getBody();
    assertNotNull(body);
    assertEquals(404, response.getStatusCode().value());
    assertEquals(ErrorType.FACHDIENST, body.getType());
  }

  @Test
  void handleMissingFhirFieldErrors() {
    val ex = new MissingFieldException(KbvErpMedication.class, "PZN");
    val response = handler.handleMissingFhirFieldErrors(ex);
    assertEquals(ErrorType.ERPIONE, response.getType());
  }
}
