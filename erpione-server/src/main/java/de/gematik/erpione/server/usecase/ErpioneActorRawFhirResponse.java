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

package de.gematik.erpione.server.usecase;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.erpione.server.actor.ErpioneActor;
import lombok.val;
import org.hl7.fhir.r4.model.Resource;

public interface ErpioneActorRawFhirResponse<A extends ErpioneActor, R extends Resource> {

  /**
   * Executes the use case with the given actor and return the raw FHIR resource.
   *
   * @return a result of type R, which is the raw FHIR output of the use case
   */
  R performAs(A actor);

  default String performAndEncodeAs(A actor) {
    return performAndEncodeAs(actor, EncodingType.XML);
  }

  default String performAndEncodeAs(A actor, EncodingType encoding) {
    val resource = performAs(actor);
    return actor.getHermes().encode(resource, encoding);
  }
}
