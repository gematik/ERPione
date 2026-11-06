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

package de.gematik.erpione.server.controller.faker;

import static de.gematik.erpione.server.controller.OpenApiETags.FAKER_TAG;
import static java.util.Objects.requireNonNullElseGet;

import de.gematik.erpione.data.r4.EMedication;
import de.gematik.erpione.data.r4.etype.MedicationEType;
import de.gematik.erpione.data.r4.kbv.*;
import de.gematik.erpione.server.controller.FhirMediaType;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.r4.kbv.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tags(value = {@Tag(name = FAKER_TAG)})
@RequestMapping("/api/faker/kbv")
@RequiredArgsConstructor
@SuppressWarnings("java:S3752")
public class KbvFakerController {

  private final HermesService hermesService;

  @Operation(
      summary = "Fake KBV-ERP-Bundle for pharmaceutical prescription",
      description = "Creates a 'KbvErpBundle' FHIR resource from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = {"/pharmaceutical"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {MediaType.APPLICATION_XML_VALUE, FhirMediaType.APPLICATION_FHIR_XML})
  public String createPharmaceuticalsPrescription(
      @RequestBody(required = false) KbvEPrescriptionIn body) {
    body = requireNonNullElseGet(body, KbvEPrescriptionIn::new);
    return hermesService.toFhirString(KbvErpBundle.class, body);
  }

  @Operation(
      summary = "Fake Hermes-Prescription-Bundle for pharmaceutical prescription",
      description = "Creates a 'KbvEPrescription' Hermes object from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = {"/pharmaceutical"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public KbvEPrescriptionOut createPharmaceuticalsPrescriptionAsHermesJson(
      @RequestBody(required = false) KbvEPrescriptionIn body) {
    body = requireNonNullElseGet(body, KbvEPrescriptionIn::new);
    val fhir = hermesService.toFhirResource(KbvErpBundle.class, body);
    return hermesService.toJson(KbvEPrescriptionOut.class, fhir);
  }

  @Operation(
      summary = "Fake KBV-EVDGA-Bundle for DiGA prescription",
      description = "Creates a 'KbvEvdgaBundle' FHIR resource from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = {"/evdga", "/diga"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {MediaType.APPLICATION_XML_VALUE, FhirMediaType.APPLICATION_FHIR_XML})
  public String createEvdgaPrescription(@RequestBody(required = false) KbvEPrescriptionIn body) {
    body = requireNonNullElseGet(body, KbvEPrescriptionIn::new);
    return hermesService.toFhirString(KbvEvdgaBundle.class, body);
  }

  @Operation(
      summary = "Fake Hermes-Prescription-Bundle for DiGA prescription",
      description = "Creates a 'KbvEPrescription' Hermes object from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = {"/evdga", "/diga"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public KbvEPrescriptionOut createEvdgaPrescriptionAsHermesJson(
      @RequestBody(required = false) KbvEPrescriptionIn body) {
    body = requireNonNullElseGet(body, KbvEPrescriptionIn::new);
    val fhir = hermesService.toFhirResource(KbvEvdgaBundle.class, body);
    return hermesService.toJson(KbvEPrescriptionOut.class, fhir);
  }

  @Operation(
      summary = "KBV Medication",
      description = "Creates a 'Medication' FHIR resource from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = {"/medication"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {MediaType.APPLICATION_XML_VALUE, FhirMediaType.APPLICATION_FHIR_XML})
  public String createMedication(
      @RequestParam(value = "type", required = false) MedicationEType type,
      @RequestBody(required = false) EMedication medicationData) {
    log.trace("Fake KbvErpMedication with template: {}", medicationData);
    val data = requireNonNullElseGet(medicationData, EMedication::new);

    Optional.ofNullable(type).ifPresent(data::setType);
    return hermesService.toFhirString(KbvErpMedication.class, data);
  }

  @Operation(
      summary = "Fake KBV-MedicationRequest",
      description = "Creates a 'MedicationRequest' FHIR resource from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = {"/medication-request"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {MediaType.APPLICATION_XML_VALUE, FhirMediaType.APPLICATION_FHIR_XML})
  public String createMedicationRequest(
      @RequestBody(required = false) KbvEMedicationRequestIn data) {
    log.trace("Fake KbvMedicationRequest with template: {}", data);
    data =
        requireNonNullElseGet(
            data,
            () -> {
              // randomly inject MVO on GET request without a template
              val mr = new KbvEMedicationRequestIn();

              if (GemFaker.getFaker().bool().bool()) {
                mr.setMvo(new KbvMvoEData());
              }
              return mr;
            });
    return hermesService.toFhirString(KbvErpMedicationRequest.class, data);
  }

  @Operation(
      summary = "Fake KBV-Patient",
      description = "Creates a 'Patient' FHIR resource from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = "/patient",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {MediaType.APPLICATION_XML_VALUE, FhirMediaType.APPLICATION_FHIR_XML})
  public String createPatient(@RequestBody(required = false) KbvEPatientIn data) {
    log.trace("Fake KbvPatient with template: {}", data);
    data = requireNonNullElseGet(data, KbvEPatientIn::new);
    return hermesService.toFhirString(KbvPatient.class, data);
  }

  @Operation(
      summary = "Fake KBV-Coverage",
      description = "Creates a 'Coverage' FHIR resource from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = "/coverage",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {MediaType.APPLICATION_XML_VALUE, FhirMediaType.APPLICATION_FHIR_XML})
  public String createCoverage(@RequestBody(required = false) KbvECoverageIn data) {
    data = requireNonNullElseGet(data, KbvECoverageIn::new);
    return hermesService.toFhirString(KbvCoverage.class, data);
  }

  @Operation(
      summary = "Fake KBV-Practitioner",
      description = "Creates a 'Practitioner' FHIR resource from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = "/practitioner",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {MediaType.APPLICATION_XML_VALUE, FhirMediaType.APPLICATION_FHIR_XML})
  public String createPractitioner(@RequestBody(required = false) KbvEPractitionerIn data) {
    data = requireNonNullElseGet(data, KbvEPractitionerIn::new);
    return hermesService.toFhirString(KbvPractitioner.class, data);
  }

  @Operation(
      summary = "Fake KBV-Organization",
      description = "Creates a 'Organization' FHIR resource from an input hermes template")
  @RequestMapping(
      method = {RequestMethod.POST, RequestMethod.GET},
      path = {"/organization"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {MediaType.APPLICATION_XML_VALUE, FhirMediaType.APPLICATION_FHIR_XML})
  public String createOrganization(@RequestBody(required = false) KbvEOrganizationIn data) {
    data = requireNonNullElseGet(data, KbvEOrganizationIn::new);
    return hermesService.toFhirString(KbvMedicalOrganization.class, data);
  }
}
