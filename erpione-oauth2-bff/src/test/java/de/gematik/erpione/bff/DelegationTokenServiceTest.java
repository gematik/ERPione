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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class DelegationTokenServiceTest {

  @Test
  void createToken_keepsExpiresInAsEffectiveTtl_andExposesRequestedTtl() {
    JwtEncoder jwtEncoder = mock(JwtEncoder.class);
    Jwt encodedJwt = mock(Jwt.class);
    when(jwtEncoder.encode(any())).thenReturn(encodedJwt);
    when(encodedJwt.getTokenValue()).thenReturn("delegation-token");

    val service =
        new DelegationTokenService(
            jwtEncoder,
            900,
            7200,
            "erpione-bff",
            "erpione-headless",
            new DelegationRelayTokenStore());

    val request = new DelegationTokenRequest(List.of("openid"), "erpione-headless", 900L);
    val authentication = new TestingAuthenticationToken("subject-user", "n/a", "SCOPE_openid");
    val authorizedClient = createAuthorizedClientWithAccessTokenTtlSeconds(270);

    val response = service.createToken(request, authentication, null, authorizedClient);

    assertThat(response.requestedExpiresIn()).isEqualTo(900);
    assertThat(response.expiresIn()).isBetween(1L, 270L);
  }

  private static OAuth2AuthorizedClient createAuthorizedClientWithAccessTokenTtlSeconds(
      long ttlSeconds) {
    val now = Instant.now();
    val registration =
        ClientRegistration.withRegistrationId("oidc")
            .clientId("client")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("https://idp.local/auth")
            .tokenUri("https://idp.local/token")
            .build();
    val accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "source-access-token",
            now.minusSeconds(30),
            now.plusSeconds(ttlSeconds),
            Set.of("openid"));
    return new OAuth2AuthorizedClient(registration, "subject-user", accessToken);
  }
}
