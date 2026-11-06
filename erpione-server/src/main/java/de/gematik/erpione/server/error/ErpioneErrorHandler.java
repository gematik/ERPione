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

import static java.text.MessageFormat.format;

import de.gematik.bbriccs.fhir.builder.exceptions.BuilderException;
import de.gematik.bbriccs.fhir.codec.EmptyResource;
import de.gematik.bbriccs.fhir.coding.exceptions.MissingFieldException;
import de.gematik.erpione.data.error.ErrorData;
import de.gematik.erpione.data.error.ErrorType;
import de.gematik.erpione.data.error.IssueData;
import de.gematik.erpione.data.exception.InvalidCodeValueException;
import de.gematik.erpione.server.hermes.error.ErpResponseExpectationError;
import de.gematik.erpione.server.hermes.error.HermesTransformerNotFoundError;
import de.gematik.idp.client.IdpClientRuntimeException;
import de.gematik.test.erezept.client.vau.VauException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class ErpioneErrorHandler {

  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(ErpioneNotFoundError.class)
  public ErrorData handleActorNotFoundException(ErpioneNotFoundError ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NoResourceFoundException.class)
  public ErrorData handleResourceNotFoundException(NoResourceFoundException ex) {
    val issue = new IssueData();
    issue.setSeverity("ERROR");
    issue.setDetails(
        format("No controller found for: {0} {1}", ex.getHttpMethod(), ex.getResourcePath()));

    return ErrorData.internalError(ex.getMessage()).withIssues(issue);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
  @ExceptionHandler(UnsupportedOperationException.class)
  public ErrorData handleSearchQueryExpectationError(UnsupportedOperationException ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
  @ExceptionHandler(HttpServerErrorException.NotImplemented.class)
  public ErrorData handleSearchQueryExpectationError(HttpServerErrorException.NotImplemented ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(ImpossibleOperationError.class)
  public ErrorData handleImpossibleOperationError(ImpossibleOperationError ex) {
    val error = internalErrorData(ex);
    error.getIssues().addAll(0, ex.getIssues());

    return error;
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(InvalidCodeValueException.class)
  public ErrorData handleInvalidCodeValueError(InvalidCodeValueException ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ErrorData handleRequestParserError(HttpMessageNotReadableException ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Throwable.class)
  public ErrorData handleUncaughtExceptions(Exception ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  @ExceptionHandler(VauException.class)
  public ErrorData handleVauConnectionError(VauException ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ErrorData handleMethodArgumentTypeMissmatch(MethodArgumentTypeMismatchException ex) {
    val data = new ErrorData();
    val expectedType =
        Optional.ofNullable(ex.getRequiredType()).map(Class::getSimpleName).orElse("unknown");
    data.setType(ErrorType.ERPIONE);
    data.setMessage(format("{0} could not be converted to {1}", ex.getValue(), expectedType));

    return data;
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(HermesTransformerNotFoundError.class)
  public ErrorData handleHermesError(HermesTransformerNotFoundError ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(BuilderException.class)
  public ErrorData handleFhirBuilderException(BuilderException ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
  @ExceptionHandler(IdpClientRuntimeException.class)
  public ErrorData handleOfflineModeException(IdpClientRuntimeException ex) {
    return internalErrorData(ex);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(ErpResponseExpectationError.class)
  public ResponseEntity<ErrorData> handleErpResponseExpectationError(
      ErpResponseExpectationError ex) {
    val data = new ErrorData();
    data.setType(ErrorType.FACHDIENST);
    data.setMessage(extractErrorMessage(ex));
    data.getIssues().addAll(extractDetails(ex));
    return ResponseEntity.status(ex.getStatusCode()).body(data);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MissingFieldException.class)
  public ErrorData handleMissingFhirFieldErrors(MissingFieldException ex) {
    return internalErrorData(ex);
  }

  private ErrorData internalErrorData(Exception ex) {
    val issue = new IssueData();
    issue.setSeverity("ERROR");
    issue.setDetails(format("{0}: {1}", ex.getClass().getSimpleName(), ex.getMessage()));

    Optional.ofNullable(ex.getCause())
        .map(cause -> cause.getClass().getSimpleName())
        .ifPresent(cause -> issue.setDiagnostics(format("caused by: {0}", cause)));

    log.error("Internal Erpione error:", ex);
    return ErrorData.internalError(ex.getMessage()).withIssues(issue);
  }

  private String extractErrorMessage(ErpResponseExpectationError ex) {
    val expected = ex.getExpected().getSimpleName();
    val actual =
        Optional.ofNullable(ex.getActual()).orElse(new EmptyResource()).getClass().getSimpleName();
    val actor = ex.getActor();
    return format(
        "Actor {0} expected a response of type ''{1}'' but received ''{2}'' instead",
        actor, expected, actual);
  }

  private List<IssueData> extractDetails(ErpResponseExpectationError ex) {
    if (ex.getActual() instanceof OperationOutcome oo) {
      return oo.getIssue().stream()
          .map(
              it -> {
                val severity = it.getSeverity().getDisplay().toUpperCase();
                val details = it.getDetails().getText();
                val diagnostics = it.getDiagnostics();

                return new IssueData(severity, details, diagnostics);
              })
          .toList();
    } else {
      return List.of();
    }
  }
}
