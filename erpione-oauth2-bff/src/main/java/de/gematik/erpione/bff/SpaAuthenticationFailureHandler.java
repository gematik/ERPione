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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import reactor.core.publisher.Mono;

@Slf4j
public class SpaAuthenticationFailureHandler implements ServerAuthenticationFailureHandler {

  private final String spaRedirectUri;

  public SpaAuthenticationFailureHandler(String spaRedirectUri) {
    this.spaRedirectUri = spaRedirectUri;
  }

  @Override
  public Mono<Void> onAuthenticationFailure(
      WebFilterExchange webFilterExchange, AuthenticationException exception) {
    return Mono.fromRunnable(
        () -> {
          log.warn("OAuth2 login failed: {}", exception.getMessage(), exception);
          webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.FOUND);
          webFilterExchange
              .getExchange()
              .getResponse()
              .getHeaders()
              .setLocation(URI.create(buildFailureRedirectUri()));
        });
  }

  private String buildFailureRedirectUri() {
    return spaRedirectUri + (spaRedirectUri.contains("?") ? "&" : "?") + "login_error=oauth2";
  }
}
