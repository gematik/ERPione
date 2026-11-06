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

package de.gematik.erpione.server.controller.eu;

import static de.gematik.erpione.server.controller.OpenApiETags.EU_PHARMACY_TAG;

import de.gematik.erpione.data.r4.eu.EuPrescriptionDispensationBatchOut;
import de.gematik.erpione.data.r4.eu.EuPrescriptionDispensationListIn;
import de.gematik.erpione.data.r4.eu.EuPrescriptionsClaim;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionList;
import de.gematik.erpione.server.actor.ErpioneActorService;
import de.gematik.erpione.server.usecase.CloseEuPharmaceuticals;
import de.gematik.erpione.server.usecase.ReadEuPrescriptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = EU_PHARMACY_TAG)
@RequestMapping("/api/eu")
@RequiredArgsConstructor
public class EuPharmacyController {

  private final ErpioneActorService actorService;

  @Operation(
      summary = "Read demographic data of a patient",
      description = "perform $get-eu-prescriptions operation for the given KVNR")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Demographic data of the patient read successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(path = "/demographics/{kvnr}", produces = MediaType.APPLICATION_JSON_VALUE)
  public KbvEPrescriptionList readEuDemographicsData(
      @RequestHeader("x-actor") String actorId,
      @PathVariable String kvnr,
      @RequestParam(value = "ac") String accessCode) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    return ReadEuPrescriptions.demographicsFor(kvnr, accessCode).performAs(actor);
  }

  @Operation(
      summary = "Read prescriptions",
      description = "perform $get-eu-prescriptions operation for the given KVNR")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Prescriptions list read successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(path = "/prescriptions/{kvnr}", produces = MediaType.APPLICATION_JSON_VALUE)
  public KbvEPrescriptionList readEuPrescriptionsList(
      @RequestHeader("x-actor") String actorId,
      @PathVariable String kvnr,
      @RequestParam(value = "ac") String accessCode) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    return ReadEuPrescriptions.prescriptionsListFor(kvnr, accessCode).performAs(actor);
  }

  @Operation(
      summary = "Claim EU prescriptions",
      description = "perform $get-eu-prescriptions operation for the given KVNR")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "claimed list of prescription ids successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = "/claim/{kvnr}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public KbvEPrescriptionList claimEuPrescriptions(
      @RequestHeader("x-actor") String actorId,
      @PathVariable String kvnr,
      @RequestParam(value = "ac") String accessCode,
      @RequestBody EuPrescriptionsClaim body) {
    val actor = actorService.getInstitutionOrThrow(actorId);
    return ReadEuPrescriptions.claimPrescriptions(kvnr, accessCode, body.getClaims())
        .performAs(actor);
  }

  @Operation(
      summary = "Close EU prescriptions",
      description =
          "perform $get-eu-prescriptions operation for the given KVNR") // TODO: api description
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "closed list of prescription ids successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = "/close/{kvnr}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public EuPrescriptionDispensationBatchOut closeEuPrescriptions(
      @RequestHeader("x-actor") String actorId,
      @PathVariable String kvnr,
      @RequestParam(value = "ac") String accessCode,
      @RequestBody EuPrescriptionDispensationListIn body) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    return CloseEuPharmaceuticals.using(kvnr, accessCode, body).performAs(actor);
  }
}
