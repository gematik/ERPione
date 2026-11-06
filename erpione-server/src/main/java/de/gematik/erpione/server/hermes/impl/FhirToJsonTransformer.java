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

package de.gematik.erpione.server.hermes.impl;

import de.gematik.erpione.data.ErpioneOutputData;
import org.hl7.fhir.r4.model.Resource;

public interface FhirToJsonTransformer<J extends ErpioneOutputData, R extends Resource>
    extends HermesTransformer<J, R> {

  /**
   * Transforms a FHIR resource to a Hermes output object.
   *
   * @param resource the FHIR resource to be transformed
   * @return a JSON object containing the data from the FHIR resource
   */
  J toJson(R resource);
}
