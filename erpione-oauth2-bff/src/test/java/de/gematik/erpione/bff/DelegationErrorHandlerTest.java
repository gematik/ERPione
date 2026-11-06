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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.erpione.data.error.ErrorType;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

class DelegationErrorHandlerTest {

  private final DelegationErrorHandler handler = new DelegationErrorHandler();

  @Test
  void responseStatusException_isReturnedAsErrorDataErpione() {
    val ex =
        new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "scope must contain at least one value");

    val response = handler.handleResponseStatusException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getType()).isEqualTo(ErrorType.ERPIONE);
    assertThat(response.getBody().getMessage()).isEqualTo("scope must contain at least one value");
    assertThat(response.getBody().getIssues()).isNotEmpty();
  }

  @Test
  void serverWebInputException_isReturnedAsErrorDataErpione() {
    val ex = new ServerWebInputException("Failed to read HTTP message");

    val response = handler.handleServerWebInputException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getType()).isEqualTo(ErrorType.ERPIONE);
    assertThat(response.getBody().getMessage()).isEqualTo("Failed to read HTTP message");
    assertThat(response.getBody().getIssues()).isNotEmpty();
  }
}
