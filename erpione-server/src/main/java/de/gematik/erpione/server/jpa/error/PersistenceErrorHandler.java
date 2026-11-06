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

package de.gematik.erpione.server.jpa.error;

import de.gematik.erpione.data.error.ErrorData;
import de.gematik.erpione.data.error.ErrorType;
import lombok.val;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class PersistenceErrorHandler {

  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(InvalidDataAccessApiUsageException.class)
  public ErrorData handleDataAccessApiUsageError(InvalidDataAccessApiUsageException ex) {
    val data = new ErrorData();
    data.setType(ErrorType.ERPIONE);
    data.setMessage(ex.getMessage());
    return data;
  }
}
