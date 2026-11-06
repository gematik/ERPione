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

import static de.gematik.erpione.server.controller.OpenApiETags.ERP_TAG;
import static de.gematik.erpione.server.controller.OpenApiETags.PHARMACY_TAG;
import static de.gematik.erpione.server.controller.OpenApiETags.PRESCRIPTION_DB_TAG;

import de.gematik.erpione.data.r4.gem.ErpETask;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.erpione.server.jpa.entity.gem.ErpEReceiptEntity;
import de.gematik.erpione.server.jpa.entity.gem.ErpETaskEntity;
import de.gematik.erpione.server.util.DataMatrixCodeGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.Like;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tags(value = {@Tag(name = ERP_TAG), @Tag(name = PRESCRIPTION_DB_TAG)})
@RequestMapping(
    path = {"/api/erp/prescriptions"},
    produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ErpPrescriptionController {

  private final ErpionePersistence repository;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<ErpETask> findPrescriptions(
      @And({
            @Spec(params = "flowType", path = "flowType", spec = Like.class),
            @Spec(params = "status", path = "status", spec = Equal.class),
            @Spec(params = "kvnr", path = "prescription.patient.kvnr", spec = Equal.class),
            @Spec(params = "iknr", path = "prescription.coverage.iknr", spec = Equal.class),
            @Spec(params = "tid", path = "signature.telematikId", spec = Equal.class),
          })
          Specification<ErpETaskEntity> spec,
      @PageableDefault(sort = "authoredOn", direction = Sort.Direction.DESC, size = 20)
          Pageable pageable) {
    log.trace("find prescriptions with {}", pageable);
    return repository.searchPrescriptions(spec, pageable);
  }

  @Operation(
      summary = "Fetch prescription receipts",
      description = "Fetch the the list of known receipts")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Prescription bundle fetched successfully"),
      })
  @Tag(name = PHARMACY_TAG)
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(path = "/receipts", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<ErpEReceiptEntity> fetchReceipts(Pageable pageable) {
    return repository.getReceipts(pageable);
  }

  @GetMapping(path = "/{pid}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpETask getPrescription(@PathVariable("pid") String prescriptionId) {
    log.trace("GET prescription by ID {}", prescriptionId);
    return repository.getPrescriptionByIdOrThrow(prescriptionId);
  }

  @Operation(
      summary = "Generate Data Matrix Code for an e-Prescription",
      description = "Generate Data Matrix Code for an e-Prescription")
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(path = "/dmc/{pid}", produces = MediaType.IMAGE_PNG_VALUE)
  public byte[] generateDataMatrixCode(
      @PathVariable("pid") String prescriptionId, @RequestParam(value = "ac") String accessCode) {

    return DataMatrixCodeGenerator.toByteArray(prescriptionId, accessCode);
  }
}
