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

package de.gematik.erpione.server.hermes.error;

import static java.text.MessageFormat.format;

import de.gematik.bbriccs.rest.fd.FhirBResponse;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Resource;
import org.jetbrains.annotations.Nullable;

@Getter
public class ErpResponseExpectationError extends RuntimeException {

  private final Class<? extends Resource> expected;
  @Nullable private final Resource actual;
  private final String actor;
  private final int statusCode;

  public ErpResponseExpectationError(String actor, FhirBResponse<?> response) {
    this(actor, response.getStatusCode(), response.getExpectedType(), response.getAsBaseResource());
  }

  public ErpResponseExpectationError(
      String actor, int statusCode, Class<? extends Resource> expected, @Nullable Resource actual) {
    super(
        format(
            "Unexpected ERP response ({0}): expected ''{1}'' but received ''{2}''",
            statusCode,
            expected.getSimpleName(),
            Optional.ofNullable(actual).map(it -> it.getClass().getSimpleName()).orElse("NULL")));
    this.expected = expected;
    this.actual = actual;
    this.actor = actor;
    this.statusCode = statusCode;
  }
}
