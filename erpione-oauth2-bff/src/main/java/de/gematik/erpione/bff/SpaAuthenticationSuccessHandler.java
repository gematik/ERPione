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

package de.gematik.erpione.bff;

import java.net.URI;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;

public class SpaAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

  private final String spaRedirectUri;

  public SpaAuthenticationSuccessHandler(String spaRedirectUri) {
    this.spaRedirectUri = spaRedirectUri;
  }

  @Override
  public Mono<Void> onAuthenticationSuccess(
      WebFilterExchange exchange, Authentication authentication) {
    return Mono.fromRunnable(
        () -> {
          exchange
              .getExchange()
              .getResponse()
              .setStatusCode(org.springframework.http.HttpStatus.FOUND);
          exchange.getExchange().getResponse().getHeaders().setLocation(URI.create(spaRedirectUri));
        });
  }
}
