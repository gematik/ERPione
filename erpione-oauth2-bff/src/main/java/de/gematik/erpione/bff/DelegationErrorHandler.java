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

package de.gematik.erpione.bff;

import de.gematik.erpione.data.error.ErrorData;
import de.gematik.erpione.data.error.IssueData;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

/** Maps delegation endpoint errors to the shared ErrorData format. */
@RestControllerAdvice(assignableTypes = DelegationController.class)
public class DelegationErrorHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorData> handleResponseStatusException(ResponseStatusException ex) {
    return toErrorResponse(ex.getStatusCode(), bestMessage(ex.getReason(), ex.getMessage()), ex);
  }

  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<ErrorData> handleServerWebInputException(ServerWebInputException ex) {
    return toErrorResponse(
        HttpStatus.BAD_REQUEST, bestMessage(ex.getReason(), ex.getMessage()), ex);
  }

  private static ResponseEntity<ErrorData> toErrorResponse(
      HttpStatusCode status, String message, Exception ex) {
    val issue = new IssueData();
    issue.setSeverity("ERROR");
    issue.setDetails(ex.getClass().getSimpleName() + ": " + message);
    if (ex.getCause() != null) {
      issue.setDiagnostics(
          ex.getCause().getClass().getSimpleName() + ": " + ex.getCause().getMessage());
    }

    val data = ErrorData.internalError(message).withIssues(issue);
    return ResponseEntity.status(status).body(data);
  }

  private static String bestMessage(String preferred, String fallback) {
    if (StringUtils.hasText(preferred)) {
      return preferred;
    }
    if (StringUtils.hasText(fallback)) {
      return fallback;
    }
    return "Bad request";
  }
}
