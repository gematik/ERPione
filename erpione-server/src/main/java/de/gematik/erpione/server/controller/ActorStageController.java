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

package de.gematik.erpione.server.controller;

import static de.gematik.erpione.server.controller.OpenApiETags.META_INFO_TAG;

import de.gematik.erpione.data.actor.ErpioneInstitutionData;
import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.server.actor.ErpioneActorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = META_INFO_TAG)
@RestController
@RequestMapping("/api/actors")
@RequiredArgsConstructor
public class ActorStageController {

  private final ErpioneActorService actorService;

  @Operation(
      summary = "Actors Information",
      description = "Get information about all available actors")
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ErpioneInstitutionData> getActors(
      @RequestParam(name = "role", required = false) InstitutionERole role) {
    return Optional.ofNullable(role)
        .map(actorService::getInstitutionByRole)
        .orElseGet(actorService::getActorsInformation);
  }

  @Operation(summary = "Actor Information", description = "Get information about an actor")
  @GetMapping(path = "/{actorId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpioneInstitutionData getActorById(@PathVariable("actorId") String actorId) {
    return actorService.getInstitutionOrThrow(actorId).getInfo();
  }
}
