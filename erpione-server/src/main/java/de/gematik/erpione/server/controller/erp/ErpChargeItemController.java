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

import static de.gematik.erpione.server.controller.OpenApiETags.CHARGE_ITEM_TAG;
import static de.gematik.erpione.server.controller.OpenApiETags.PHARMACY_TAG;
import static java.util.Optional.ofNullable;

import de.gematik.erpione.data.r4.chargeitem.ErpEChargeItemOut;
import de.gematik.erpione.data.r4.chargeitem.InvoiceEDataIn;
import de.gematik.erpione.data.r4.chargeitem.InvoiceEType;
import de.gematik.erpione.data.r4.chargeitem.TotalEGrossIn;
import de.gematik.erpione.server.actor.ErpioneActorService;
import de.gematik.erpione.server.controller.FhirMediaType;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.usecase.ChargeItemUsecase;
import de.gematik.test.erezept.fhir.r4.dav.DavInvoice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@Tags(value = {@Tag(name = CHARGE_ITEM_TAG), @Tag(name = PHARMACY_TAG)})
@RequestMapping("/api/erp")
@RequiredArgsConstructor
public class ErpChargeItemController {

  private final ErpioneActorService actorService;
  private final HermesService hermesService;

  @Operation(
      summary = "Get the ChargeItem for PID",
      description = "Get the ChargeItem by its corresponding prescription ID (PID)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "ChargeItem fetched successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(
      path = {"/chargeitems/{pid}"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpEChargeItemOut getChargeItem(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestParam("ac") String chargeItemAccessCode) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    return ChargeItemUsecase.getForPrescription(prescriptionId, chargeItemAccessCode)
        .performAs(actor);
  }

  @Operation(
      summary = "Create ChargeItem for PID",
      description = "Create a new ChargeItem by its corresponding prescription ID (PID)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "ChargeItem fetched successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = {"/chargeitems/{pid}"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpEChargeItemOut postChargeItems(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestBody(required = false) InvoiceEDataIn data) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    data =
        ofNullable(data)
            .orElseGet(
                () ->
                    new InvoiceEDataIn(
                        "EUR", InvoiceEType.ABRECHNUNGSZEILEN, new TotalEGrossIn(), List.of()));
    val invoice = hermesService.toFhirResource(DavInvoice.class, data);

    return ChargeItemUsecase.createForPrescription(prescriptionId, invoice).performAs(actor);
  }

  @Operation(
      summary = "Create ChargeItem for PID with Fhir Bundle",
      description = "Create a new ChargeItem by its corresponding prescription ID (PID)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "ChargeItem fetched successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = {"/chargeitems/{pid}"},
      consumes = FhirMediaType.APPLICATION_FHIR_XML,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpEChargeItemOut postChargeItemsWithFhir(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestBody(required = false) String data) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    val invoice = hermesService.decode(DavInvoice.class, data);

    return ChargeItemUsecase.createForPrescription(prescriptionId, invoice).performAs(actor);
  }

  @Operation(
      summary = "Change the existing ChargeItem for PID",
      description =
          "Change the existing ChargeItem by its corresponding prescription ID (PID) and an access"
              + " code provided by the FdV")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "ChargeItem updated successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PutMapping(
      path = {"/chargeitems/{pid}"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpEChargeItemOut putChargeItem(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestParam("ac") String chargeItemAccessCode,
      @RequestBody(required = false) InvoiceEDataIn data) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    data =
        ofNullable(data)
            .orElseGet(
                () ->
                    new InvoiceEDataIn(
                        "EUR",
                        InvoiceEType.ABRECHNUNGSZEILEN,
                        new TotalEGrossIn(42.0F, 99.0F),
                        List.of()));
    val invoice = hermesService.toFhirResource(DavInvoice.class, data);

    return ChargeItemUsecase.updateForPrescription(prescriptionId, invoice, chargeItemAccessCode)
        .performAs(actor);
  }

  @Operation(
      summary = "Change the existing ChargeItem for PID with an existing Fhir Bundle",
      description =
          "Change the existing ChargeItem by its corresponding prescription ID (PID) and an access"
              + " code provided by the FdV")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "ChargeItem updated successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PutMapping(
      path = {"/chargeitems/{pid}"},
      consumes = FhirMediaType.APPLICATION_FHIR_XML,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpEChargeItemOut putChargeItemWithFhir(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestParam("ac") String chargeItemAccessCode,
      @RequestBody(required = false) String data) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    val invoice = hermesService.decode(DavInvoice.class, data);

    return ChargeItemUsecase.updateForPrescription(prescriptionId, invoice, chargeItemAccessCode)
        .performAs(actor);
  }
}
