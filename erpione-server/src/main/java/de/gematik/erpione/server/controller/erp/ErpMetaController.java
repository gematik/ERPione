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

package de.gematik.erpione.server.controller.erp;

import static de.gematik.erpione.server.controller.OpenApiETags.*;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.erpione.data.meta.TIHealthCheckResult;
import de.gematik.erpione.server.actor.ErpioneActorService;
import de.gematik.test.erezept.client.usecases.GetCapabilityStatementCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tags(
    value = {
      @Tag(name = ERP_TAG),
      @Tag(name = META_INFO_TAG),
      @Tag(name = DOCTOR_TAG),
      @Tag(name = PHARMACY_TAG),
      @Tag(name = EU_PHARMACY_TAG),
      @Tag(name = KTR_TAG)
    })
@RequestMapping("/api/erp")
@RequiredArgsConstructor
public class ErpMetaController {

  private final ErpioneActorService actorService;

  @Operation(
      summary = "e-Prescription server meta",
      description = "Read the meta information (CapabilityStatement) of the e-Prescription server")
  @GetMapping(path = "/meta", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getFachdienstInfo(@RequestHeader("x-actor") String actorId) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    val cs = actor.requestAndUnpack(new GetCapabilityStatementCommand());
    val csXml = actor.getHermes().encode(cs, EncodingType.JSON);
    return ResponseEntity.ofNullable(csXml);
  }

  @Operation(
      summary = "e-Prescription server healthcheck",
      description =
          "Probe the e-Prescription server as described in <a"
              + " href=\"https://github.com/gematik/api-erp/blob/master/docs/erp_ps_probing.adoc#einfacher-health-check\">the"
              + " e-Prescription API description</a>")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Healthcheck performed successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TIHealthCheckResult> getFachdienstHealthcheck(
      @RequestHeader("x-actor") String actorId) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    val result = actor.erpHealthCheck();
    return ResponseEntity.ok().body(result);
  }
}
