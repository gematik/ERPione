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

import com.ibm.icu.impl.Pair;
import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.smartcards.Hba;
import de.gematik.erpione.data.r4.gem.ErpESignature;
import lombok.val;
import org.hl7.fhir.r4.model.Resource;

/**
 * HealthcareProvider is a marker interface for actors that represent healthcare providers
 * (Leistungserbringer) in the healthcare system.
 */
public interface HealthcareProvider extends ErpioneActor {

  Hba getHba();

  /**
   * Telematik ID of the healthcare provider (LE) from his/her <b>HBA</b>
   *
   * @return the TelematikId
   */
  String getTelematikId();

  /**
   * sign a plain string with the actor's signing smartcard
   *
   * @param document
   * @return
   */
  Pair<byte[], ErpESignature> signDocument(String document);

  /**
   * sign a FHIR resource (encoded as XML) with the actor's signing smartcard
   *
   * @param resource
   * @return
   */
  default Pair<byte[], ErpESignature> signDocument(Resource resource) {
    val xml = this.getHermes().encode(resource, EncodingType.XML);
    return signDocument(xml);
  }
}
