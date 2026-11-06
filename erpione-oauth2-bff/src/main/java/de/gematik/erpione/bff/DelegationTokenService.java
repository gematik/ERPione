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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DelegationTokenService {

  private final JwtEncoder jwtEncoder;
  private final long defaultTtlSeconds;
  private final long maxTtlSeconds;
  private final String issuer;
  private final String defaultAudience;
  private final DelegationRelayTokenStore relayTokenStore;

  public DelegationTokenService(
      JwtEncoder jwtEncoder,
      @Value("${bff.delegation.default-ttl-seconds:900}") long defaultTtlSeconds,
      @Value("${bff.delegation.max-ttl-seconds:3600}") long maxTtlSeconds,
      @Value("${bff.delegation.issuer:erpione-bff}") String issuer,
      @Value("${bff.delegation.default-audience:erpione-headless}") String defaultAudience,
      DelegationRelayTokenStore relayTokenStore) {
    this.jwtEncoder = jwtEncoder;
    this.defaultTtlSeconds = defaultTtlSeconds;
    this.maxTtlSeconds = maxTtlSeconds;
    this.issuer = issuer;
    this.defaultAudience = defaultAudience;
    this.relayTokenStore = relayTokenStore;
  }

  public DelegationTokenResponse createToken(
      DelegationTokenRequest request,
      Authentication authentication,
      OAuth2User principal,
      OAuth2AuthorizedClient authorizedClient) {
    if (request == null || request.scope() == null || request.scope().isEmpty()) {
      throw new ResponseStatusException(BAD_REQUEST, "scope must contain at least one value");
    }
    var requestedScopes =
        request.scope().stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(it -> !it.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (requestedScopes.isEmpty()) {
      throw new ResponseStatusException(BAD_REQUEST, "scope must contain at least one value");
    }

    var grantedScopes = extractGrantedScopes(authentication);
    if (!grantedScopes.containsAll(requestedScopes)) {
      throw new ResponseStatusException(
          FORBIDDEN, "requested scope is not contained in authenticated user scopes");
    }

    var accessTokenExpiresAt = authorizedClient.getAccessToken().getExpiresAt();
    if (accessTokenExpiresAt == null) {
      throw new ResponseStatusException(BAD_REQUEST, "OAuth2 access token has no expiration");
    }

    var requestedTtlSeconds =
        request.ttlSeconds() != null ? request.ttlSeconds() : defaultTtlSeconds;
    var accessTokenTtlSeconds =
        java.time.Duration.between(Instant.now(), accessTokenExpiresAt).getSeconds();
    var effectiveTtlSeconds = Math.min(requestedTtlSeconds, accessTokenTtlSeconds);
    if (effectiveTtlSeconds <= 0 || effectiveTtlSeconds > maxTtlSeconds) {
      throw new ResponseStatusException(
          BAD_REQUEST, "ttlSeconds must be > 0 and <= " + maxTtlSeconds);
    }

    var audience =
        request.aud() == null || request.aud().isBlank() ? defaultAudience : request.aud().trim();
    var now = Instant.now();
    var expiresAt = now.plusSeconds(effectiveTtlSeconds);
    var subject = authentication.getName();

    var claimsBuilder =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(subject)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .audience(java.util.List.of(audience))
            .id(UUID.randomUUID().toString())
            .claim("scope", String.join(" ", requestedScopes))
            .claim("scp", requestedScopes)
            .claim("delegated_by", "bff");

    if (principal != null) {
      claimsBuilder.claim("name", principal.getAttribute("name"));
      claimsBuilder.claim("preferred_username", principal.getAttribute("preferred_username"));
      claimsBuilder.claim("email", principal.getAttribute("email"));
    }

    var token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claimsBuilder.build()))
            .getTokenValue();

    relayTokenStore.put(token, authorizedClient.getAccessToken().getTokenValue(), expiresAt);

    return new DelegationTokenResponse(
        token,
        "Bearer",
        effectiveTtlSeconds,
        requestedTtlSeconds,
        String.join(" ", requestedScopes),
        subject,
        audience);
  }

  private static Set<String> extractGrantedScopes(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith("SCOPE_"))
        .map(authority -> authority.substring("SCOPE_".length()))
        .filter(scope -> !scope.isBlank())
        .collect(Collectors.toSet());
  }
}
