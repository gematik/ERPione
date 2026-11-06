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

package de.gematik.erpione.server.actor.factory;

import de.gematik.bbriccs.rest.fd.FhirBResponseCreator;
import de.gematik.bbriccs.rest.fd.MediaType;
import de.gematik.test.erezept.client.ClientType;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.vau.VauClient;
import de.gematik.test.erezept.fhir.parser.FhirParser;
import java.security.cert.X509Certificate;
import lombok.Data;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Configuration;

@Data
public class ErpConnectionConfig {

  private String fdBaseUrl;
  private String userAgent;
  private ErpioneIdpConfig idp;
  @Nullable private String xapiKey;

  @Configuration
  public static class TiConnectionConfig extends ErpConnectionConfig {

    public ErpClient createErpClient(X509Certificate vauCert, FhirParser fhirCodec) {
      val idpClient = this.getIdp().createIdpClient();
      val vauClient =
          new VauClient(
              this.getFdBaseUrl(), ClientType.PS, vauCert, this.getXapiKey(), this.getUserAgent());

      return ErpClient.builder()
          .clientType(ClientType.PS)
          .vauClient(vauClient)
          .idpClient(idpClient)
          .fhir(fhirCodec)
          .responseFactory(new FhirBResponseCreator(fhirCodec, fhirCodec::decode))
          .baseFdUrl(this.getFdBaseUrl())
          .acceptCharset("UTF-8")
          .acceptMime(MediaType.ACCEPT_FHIR_XML)
          .sendMime(MediaType.FHIR_XML)
          .validateRequest(false)
          .build();
    }
  }

  @Configuration
  public static class InternetConnectionConfig extends ErpConnectionConfig {}
}
