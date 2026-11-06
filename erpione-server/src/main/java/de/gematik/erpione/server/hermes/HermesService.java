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

package de.gematik.erpione.server.hermes;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.erpione.data.ErpioneInputData;
import de.gematik.erpione.data.ErpioneOutputData;
import lombok.NonNull;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;

public interface HermesService {

  <J extends ErpioneOutputData, R extends Resource> J toJson(Class<J> jsonType, R fhirResource);

  /**
   * Encodes a JSON object to a FHIR resource of the specified type.
   *
   * @param data
   * @param fhirType
   * @param <J>
   * @param <R>
   * @return
   */
  <J extends ErpioneInputData, R extends Resource> String toFhirString(
      Class<R> fhirType, @NonNull J data);

  /**
   * Converts a JSON object to a FHIR resource of the specified type.
   *
   * @param data
   * @param fhirType
   * @param <J>
   * @param <R>
   * @return
   */
  <J extends ErpioneInputData, R extends Resource> R toFhirResource(
      Class<R> fhirType, @NonNull J data);

  /**
   * Note on java:S1172: The builderType is required to properly distinguish the generic type of the
   * builder
   *
   * @param data
   * @param fhirType
   * @param builderType
   * @param <J>
   * @param <R>
   * @param <B>
   * @return
   */
  @SuppressWarnings({"java:S1172"})
  <J extends ErpioneInputData, R extends Resource, B extends ResourceBuilder<R, B>> B toFhirBuilder(
      J data, Class<R> fhirType, Class<B> builderType);

  <R extends Resource> R decode(Class<R> expectedClass, String content);

  <R extends Resource> R decode(Class<R> expectedClass, String content, EncodingType encoding);

  String encode(IBaseResource resource, EncodingType encoding);
}
