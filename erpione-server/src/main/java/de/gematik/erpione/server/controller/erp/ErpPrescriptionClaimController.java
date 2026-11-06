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
import de.gematik.erpione.data.error.EmptyData;
import de.gematik.erpione.data.r4.gem.ErpEReceiptOut;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.data.r4.gem.MedicationDispenseEBundleIn;
import de.gematik.erpione.server.actor.ErpioneActorService;
import de.gematik.erpione.server.controller.FhirMediaType;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.usecase.*;
import de.gematik.erpione.server.usecase.raw.ReFetchReceipt;
import de.gematik.test.erezept.fhir.r4.erp.GemCloseOperationParameters;
import de.gematik.test.erezept.fhir.r4.erp.GemDispenseOperationParameters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = ERP_TAG)
@RequestMapping("/api/erp")
@RequiredArgsConstructor
public class ErpPrescriptionClaimController {

  private final ErpioneActorService actorService;
  private final HermesService hermesService;

  @Operation(
      summary = "Delete a prescription",
      description = "perform $abort operation for the given prescription")
  @Tags(value = {@Tag(name = DOCTOR_TAG), @Tag(name = PHARMACY_TAG), @Tag(name = KTR_TAG)})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pharmaceutical e-Prescription deleted successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @DeleteMapping(path = "/{pid}/abort", produces = MediaType.APPLICATION_JSON_VALUE)
  public EmptyData abortPrescription(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestParam(value = "ac", required = false) String accessCode,
      @RequestParam(value = "secret", required = false) String secret) {
    val actor = actorService.getInstitutionOrThrow(actorId);
    return DeletePrescription.with(prescriptionId).andAccess(accessCode, secret).performAs(actor);
  }

  @Operation(
      summary = "Claim a prescription",
      description = "perform $accept operation for the given prescription")
  @Tags(value = {@Tag(name = PHARMACY_TAG), @Tag(name = KTR_TAG)})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "e-Prescription claimed successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(path = "/{pid}/claim", produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpETask acceptPrescription(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestParam("ac") String accessCode) {
    val actor = actorService.getInstitutionOrThrow(actorId);
    return ClaimPrescription.with(prescriptionId, accessCode).performAs(actor);
  }

  @Operation(
      summary = "Return prescription claim",
      description = "perform $reject operation for the given prescription")
  @Tags(value = {@Tag(name = PHARMACY_TAG), @Tag(name = KTR_TAG)})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pharmaceutical e-Prescription returned successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(path = "/{pid}/return", produces = MediaType.APPLICATION_JSON_VALUE)
  public EmptyData rejectPrescription(
      @RequestHeader("x-actor") String actorId, @PathVariable("pid") String prescriptionId) {
    val actor = actorService.getInstitutionOrThrow(actorId);
    return ReturnPrescriptionClaim.forPrescription(prescriptionId).performAs(actor);
  }

  @Operation(
      summary = "Dispense pharmaceutical prescription claim",
      description = "perform dispense operation for the given prescription")
  @Tags(value = {@Tag(name = PHARMACY_TAG)})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pharmaceutical e-Prescription dispensed successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = "/{pid}/dispense",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public EmptyData dispensePharmaceuticalPrescriptionWithHermes(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestBody(required = false) MedicationDispenseEBundleIn body) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    val params =
        Optional.ofNullable(body)
            .map(it -> hermesService.toFhirResource(GemDispenseOperationParameters.class, it))
            .orElse(null);

    return DispensePharmaceutical.with(prescriptionId).using(params).performAs(actor);
  }

  @Operation(
      summary = "Dispense pharmaceutical prescription claim",
      description = "perform dispense operation for the given prescription")
  @Tags(value = {@Tag(name = PHARMACY_TAG)})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pharmaceutical e-Prescription dispensed successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = "/{pid}/dispense",
      consumes = FhirMediaType.APPLICATION_FHIR_XML,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public EmptyData dispensePharmaceuticalPrescriptionWithFhirTemplate(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestBody String body) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    val params = hermesService.decode(GemDispenseOperationParameters.class, body);

    return DispensePharmaceutical.with(prescriptionId).using(params).performAs(actor);
  }

  @Operation(
      summary = "Close pharmaceutical prescription claim",
      description = "perform close operation for the given prescription")
  @Tags(value = {@Tag(name = PHARMACY_TAG)})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pharmaceutical e-Prescription closed successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = "{pid}/close",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpEReceiptOut closePharmaceuticalPrescriptionWithHermes(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestBody(required = false) MedicationDispenseEBundleIn body) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    val params =
        Optional.ofNullable(body)
            .map(it -> hermesService.toFhirResource(GemCloseOperationParameters.class, it))
            .orElse(null);

    return ClosePharmaceutical.with(prescriptionId).using(params).performAs(actor);
  }

  @Operation(
      summary = "Close pharmaceutical prescription claim",
      description = "perform close operation for the given prescription")
  @Tags(value = {@Tag(name = PHARMACY_TAG)})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pharmaceutical e-Prescription closed successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = "/{pid}/close",
      consumes = FhirMediaType.APPLICATION_FHIR_XML,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpEReceiptOut closePharmaceuticalPrescriptionWithFhirTemplate(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestBody String body) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    val params = hermesService.decode(GemCloseOperationParameters.class, body);

    return ClosePharmaceutical.with(prescriptionId).using(params).performAs(actor);
  }

  @Operation(
      summary = "Approve DiGA prescription",
      description = "Approve ($close) DiGA prescription")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "DiGA e-Prescription closed successfully"),
      })
  @Tags(value = {@Tag(name = KTR_TAG)})
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(path = "/{pid}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpEReceiptOut approveDiGAPrescription(
      @RequestHeader("x-actor") String actorId, @PathVariable("pid") String prescriptionId) {
    val actor = actorService.getInstitutionOrThrow(actorId);
    return CloseDiGA.approve(prescriptionId).performAs(actor);
  }

  @Operation(
      summary = "Decline DiGA prescription",
      description = "Decline ($close) DiGA prescription")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "DiGA e-Prescription closed successfully"),
      })
  @Tags(value = {@Tag(name = KTR_TAG)})
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(path = "/{pid}/decline", produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpEReceiptOut declineDiGAPrescription(
      @RequestHeader("x-actor") String actorId, @PathVariable("pid") String prescriptionId) {
    val actor = actorService.getInstitutionOrThrow(actorId);
    return CloseDiGA.decline(prescriptionId).performAs(actor);
  }

  @Operation(
      summary = "Fetch prescription bundle",
      description = "Fetch the whole prescription bundle which contains the receipt")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Prescription bundle fetched successfully"),
      })
  @Tags(value = {@Tag(name = PHARMACY_TAG)})
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(path = "/receipts/{pid}", produces = FhirMediaType.APPLICATION_FHIR_XML)
  public String fetchPrescriptionBundle(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestParam(value = "secret", required = false) String secret) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    return ReFetchReceipt.with(prescriptionId, secret).performAndEncodeAs(actor, EncodingType.XML);
  }
}
