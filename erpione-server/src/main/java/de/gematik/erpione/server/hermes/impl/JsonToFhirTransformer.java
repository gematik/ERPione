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

import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.erpione.data.ErpioneInputData;
import org.hl7.fhir.r4.model.Resource;

public interface JsonToFhirTransformer<
        J extends ErpioneInputData, R extends Resource, B extends ResourceBuilder<R, B>>
    extends HermesTransformer<J, R> {

  /**
   * transforms a Hermes input object to a FHIR resource builder
   *
   * @param data for the input of the fhir resource builder
   * @return a FHIR resource builder containing the data from the JSON object
   */
  B toFhir(J data);
}
