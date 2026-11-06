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

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class DelegationController {

  private final DelegationTokenService delegationTokenService;
  private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

  public DelegationController(
      DelegationTokenService delegationTokenService,
      ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
    this.delegationTokenService = delegationTokenService;
    this.authorizedClientManager = authorizedClientManager;
  }

  @PostMapping("/internal/delegations")
  public Mono<DelegationTokenResponse> createDelegation(
      @RequestBody DelegationTokenRequest request,
      Authentication authentication,
      @AuthenticationPrincipal OAuth2User principal) {
    if (!(authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken)) {
      return Mono.error(
          new ResponseStatusException(
              UNAUTHORIZED, "delegation tokens can only be minted from an OAuth2 login session"));
    }

    var authorizeRequest =
        OAuth2AuthorizeRequest.withClientRegistrationId(
                oauth2AuthenticationToken.getAuthorizedClientRegistrationId())
            .principal(authentication)
            .build();

    return authorizedClientManager
        .authorize(authorizeRequest)
        .switchIfEmpty(
            Mono.error(
                new ResponseStatusException(
                    UNAUTHORIZED,
                    "no OAuth2 authorized client found or refresh failed for current user"
                        + " session")))
        .map(
            authorizedClient ->
                delegationTokenService.createToken(
                    request, authentication, principal, authorizedClient));
  }
}
