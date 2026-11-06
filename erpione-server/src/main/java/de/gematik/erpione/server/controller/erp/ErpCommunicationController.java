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

import static de.gematik.erpione.server.controller.OpenApiETags.COMMUNICATION_TAG;
import static de.gematik.erpione.server.controller.OpenApiETags.KTR_TAG;
import static de.gematik.erpione.server.controller.OpenApiETags.PHARMACY_TAG;

import de.gematik.erpione.data.r4.gem.com.CommunicationEDataBundleOut;
import de.gematik.erpione.data.r4.gem.com.CommunicationEDataIn;
import de.gematik.erpione.data.r4.gem.com.CommunicationEDataOut;
import de.gematik.erpione.data.r4.gem.com.CommunicationEReferenceIn;
import de.gematik.erpione.server.actor.ErpioneActorService;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.usecase.SendCommunicationReply;
import de.gematik.test.erezept.client.rest.param.SortOrder;
import de.gematik.test.erezept.client.usecases.search.CommunicationSearch;
import de.gematik.test.erezept.fhir.r4.erp.ErxCommunication;
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
@Tags(value = {@Tag(name = COMMUNICATION_TAG), @Tag(name = PHARMACY_TAG), @Tag(name = KTR_TAG)})
@RequestMapping("/api/erp")
@RequiredArgsConstructor
public class ErpCommunicationController {

  private final ErpioneActorService actorService;
  private final HermesService hermesService;

  @Operation(
      summary = "Fetch received communications",
      description = "Fetch received communications")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Communications fetched successfully"),
      })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(
      path = {"/communications"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CommunicationEDataBundleOut fetchReceivedCommunications(
      @RequestHeader("x-actor") String actorId) {
    val actor = actorService.getInstitutionOrThrow(actorId);

    val request = CommunicationSearch.getAllCommunications(SortOrder.DESCENDING);
    val response = actor.requestAndUnpack(request);
    val out =
        response.getCommunications().stream()
            .map(it -> hermesService.toJson(CommunicationEDataOut.class, it))
            .toList();
    return new CommunicationEDataBundleOut(out);
  }

  @Operation(
      summary = "Post communication reply message",
      description = "Post communication reply message")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Communications posted successfully"),
      })
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(
      path = {"/communications/{pid}/reply"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CommunicationEDataOut postReplyMessage(
      @RequestHeader("x-actor") String actorId,
      @PathVariable("pid") String prescriptionId,
      @RequestBody CommunicationEDataIn message) {
    val m = Optional.ofNullable(message).orElseGet(CommunicationEDataIn::new);
    val ref = new CommunicationEReferenceIn(prescriptionId, null);
    ref.setPrescriptionId(prescriptionId);
    m.setReference(ref);
    return postReplyMessage(actorId, m);
  }

  @Operation(
      summary = "Post communication reply message",
      description = "Post communication reply message")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Communications posted successfully"),
      })
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(
      path = {"/communications/reply"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CommunicationEDataOut postReplyMessage(
      @RequestHeader("x-actor") String actorId, @RequestBody CommunicationEDataIn message) {
    val actor = actorService.getInstitutionOrThrow(actorId);
    val erxCom = hermesService.toFhirResource(ErxCommunication.class, message);
    return SendCommunicationReply.withCommunicationTemplate(erxCom).performAs(actor);
  }
}
