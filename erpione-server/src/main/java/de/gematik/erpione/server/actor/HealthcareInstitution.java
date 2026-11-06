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

package de.gematik.erpione.server.actor;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.erpione.data.meta.TIHealthCheckResult;
import de.gematik.test.erezept.fhir.valuesets.IsoCountryCodeNCPeH;
import lombok.val;
import org.hl7.fhir.r4.model.Resource;

/** a HealthcareInstitution (LEI) is an actor who has only an SMC-B */
public interface HealthcareInstitution extends ErpioneActor {

  IsoCountryCodeNCPeH getCountryCode();

  /**
   * Telematik ID of the healthcare institution (LEI) from its <b>SMC-B</b>
   *
   * @return the TelematikId
   */
  String getTelematikId();

  /**
   * Refreshes the IDP token for this actor. This can be used to implement asynchronous token
   * refreshes
   */
  void refreshIdpToken();

  /**
   * Perform a lightweight health check request to the ERP service to verify its availability and
   * responsiveness.
   *
   * @return the Health check result of the ERP service, including status and any relevant details.
   */
  TIHealthCheckResult erpHealthCheck();

  /**
   * sign a plain string with the actor's signing smartcard
   *
   * @param document
   * @return
   */
  byte[] signDocument(String document);

  /**
   * sign a FHIR resource (encoded as XML) with the actor's signing smartcard
   *
   * @param resource
   * @return
   */
  default byte[] signDocument(Resource resource) {
    val xml = this.getHermes().encode(resource, EncodingType.XML);
    return signDocument(xml);
  }
}
