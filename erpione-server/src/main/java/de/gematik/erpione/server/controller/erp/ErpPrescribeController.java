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

import static de.gematik.erpione.server.controller.OpenApiETags.DOCTOR_TAG;
import static de.gematik.erpione.server.controller.OpenApiETags.ERP_TAG;

import de.gematik.bbriccs.fhir.coding.WithSystem;
import de.gematik.bbriccs.fhir.de.DeBasisProfilNamingSystem;
import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.data.r4.kbv.KbvEPrescriptionIn;
import de.gematik.erpione.server.actor.ErpioneActorService;
import de.gematik.erpione.server.controller.FhirMediaType;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.usecase.PrescribeUsecase;
import de.gematik.test.erezept.fhir.r4.kbv.KbvBaseBundle;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import de.gematik.test.erezept.fhir.r4.kbv.KbvEvdgaBundle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@Tags(value = {@Tag(name = ERP_TAG), @Tag(name = DOCTOR_TAG)})
@RequestMapping("/api/erp")
@RequiredArgsConstructor
public class ErpPrescribeController {

  private final ErpioneActorService actorService;
  private final HermesService hermesService;

  @Operation(
      summary = "Prescribe pharmaceuticals",
      description = "Prescribe a pharmaceutical e-Prescription")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "e-Prescription created successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = {"/pharmaceutical"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpETask prescribePharmaceuticalsWithHermes(
      @RequestHeader("x-actor") String actorId,
      @RequestBody(required = false) KbvEPrescriptionIn body,
      @RequestParam(value = "direct", defaultValue = "false") boolean asDirectAssignment) {
    val actor = actorService.getHealthcareProviderOrThrow(actorId);

    body = Optional.ofNullable(body).orElseGet(KbvEPrescriptionIn::new);
    val prescription = hermesService.toFhirResource(KbvErpBundle.class, body);

    return PrescribeUsecase.forPharmaceutical(prescription, asDirectAssignment).performAs(actor);
  }

  @Operation(
      summary = "Prescribe pharmaceuticals with FHIR Template",
      description = "Prescribe a pharmaceutical e-Prescription")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "e-Prescription created successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = {"/pharmaceutical"},
      consumes = FhirMediaType.APPLICATION_FHIR_XML,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpETask prescribePharmaceuticalsWithFhirTemplate(
      @RequestHeader("x-actor") String actorId,
      @RequestParam(value = "direct", defaultValue = "false") boolean asDirectAssignment,
      @RequestParam(value = "kvnr", required = false) String kvnr,
      @RequestBody String body) {
    val actor = actorService.getHealthcareProviderOrThrow(actorId);

    val prescription = hermesService.decode(KbvErpBundle.class, body);
    setKvnrIfProvided(prescription, kvnr);

    return PrescribeUsecase.forPharmaceutical(prescription, asDirectAssignment).performAs(actor);
  }

  @Operation(
      summary = "Prescribe DiGAs",
      description =
          "Prescribe a DiGA e-Prescription (elektronischen Verordnung für digitale"
              + " Gesundheitsanwendungen)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "DiGA e-Prescription created successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = {"/evdga", "/diga"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpETask prescribeEvdgaWithHermes(
      @RequestHeader("x-actor") String actorId,
      @RequestBody(required = false) KbvEPrescriptionIn body) {
    val actor = actorService.getHealthcareProviderOrThrow(actorId);

    body = Objects.requireNonNullElseGet(body, KbvEPrescriptionIn::new);
    val prescription = hermesService.toFhirResource(KbvEvdgaBundle.class, body);

    return PrescribeUsecase.forDiGA(prescription).performAs(actor);
  }

  @Operation(
      summary = "Prescribe DiGAs",
      description =
          "Prescribe a DiGA e-Prescription (elektronischen Verordnung für digitale"
              + " Gesundheitsanwendungen)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "DiGA e-Prescription created successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(
      path = {"/evdga", "/diga"},
      consumes = FhirMediaType.APPLICATION_FHIR_XML,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpETask prescribeEvdgaWithFhirTemplate(
      @RequestHeader("x-actor") String actorId,
      @RequestParam(value = "kvnr", required = false) String kvnr,
      @RequestBody String body) {
    val actor = actorService.getHealthcareProviderOrThrow(actorId);

    val prescription = hermesService.decode(KbvEvdgaBundle.class, body);
    setKvnrIfProvided(prescription, kvnr);

    return PrescribeUsecase.forDiGA(prescription).performAs(actor);
  }

  private void setKvnrIfProvided(KbvBaseBundle bundle, @Nullable String kvnr) {
    // replace the KVNR of the patient in the example if one was given
    Optional.ofNullable(kvnr)
        .ifPresent(
            providedKvnr ->
                bundle.getPatient().getIdentifier().stream()
                    .filter(
                        it ->
                            WithSystem.anyOf(
                                    DeBasisProfilNamingSystem.KVID_PKV_SID,
                                    DeBasisProfilNamingSystem.KVID_GKV_SID)
                                .matches(it))
                    .forEach(it -> it.setValue(providedKvnr)));
  }
}
