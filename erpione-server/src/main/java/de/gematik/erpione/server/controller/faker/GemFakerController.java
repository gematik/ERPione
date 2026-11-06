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

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.erpione.data.r4.etype.PrescriptionEFlow;
import de.gematik.erpione.data.r4.gem.MedicationDispenseEBundleIn;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import de.gematik.test.erezept.fhir.builder.erp.FlowTypeBuilder;
import de.gematik.test.erezept.fhir.r4.erp.GemDispenseOperationParameters;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tags(value = {@Tag(name = FAKER_TAG)})
@RequestMapping("/api/faker/gem")
@RequiredArgsConstructor
public class GemFakerController {

  private final HermesService hermesService;

  @GetMapping(
      path = {"/flowtype", "/ft"},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_XML_VALUE)
  public String createPrescriptionFlowType(
      @RequestParam(value = "type", required = false) PrescriptionEFlow type) {
    val flowType =
        Optional.ofNullable(type)
            .map(it -> PrescriptionFlowType.fromCode(it.getCode()))
            .orElseGet(() -> GemFaker.fakerValueSet(PrescriptionFlowType.class));

    val param = FlowTypeBuilder.build(flowType);
    return hermesService.encode(param, EncodingType.XML);
  }

  @PostMapping(
      path = "/dispense",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_XML_VALUE)
  public String dispensePharmaceuticalPrescriptionWithHermes(
      @RequestBody(required = false) MedicationDispenseEBundleIn body) {

    val params =
        Optional.ofNullable(body)
            .map(it -> hermesService.toFhirResource(GemDispenseOperationParameters.class, it))
            .orElse(null);

    return hermesService.encode(params, EncodingType.XML);
  }
}
