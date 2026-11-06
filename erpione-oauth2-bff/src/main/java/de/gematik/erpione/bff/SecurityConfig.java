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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.HeaderSpec.FrameOptionsSpec;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

/**
 * Security configuration for the OAuth2 BFF.
 *
 * <p>Key design decisions:
 *
 * <ol>
 *   <li>Session-based auth – the SPA holds only a <em>session cookie</em>; no tokens in the
 *       browser.
 *   <li>CSRF protection via a JS-readable cookie so the SPA can send the token in a header.
 *   <li>Token Relay filter on the Gateway routes – access tokens are injected server-side.
 *   <li>OIDC back-channel logout propagates to the Redis session store.
 *   <li>All unauthenticated API calls get 401 (not a redirect) so the SPA can handle it.
 * </ol>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  /** Public paths that never require authentication. */
  private static final String[] PUBLIC_PATHS = {
    "/actuator/health",
    "/actuator/info",
    "/login",
    "/login/**",
    "/oauth2/**",
    "/logout",
    "/error",
    "/swagger-ui/**",
    "/api-docs/**",
    "/v3/api-docs/**"
  };

  @Value("${bff.post-logout-redirect-uri:http://localhost:4200}")
  private String postLogoutRedirectUri;

  // ── Beans ─────────────────────────────────────────────────────────────

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(
      ServerHttpSecurity http,
      ReactiveClientRegistrationRepository clientRegistrationRepository,
      ReactiveJwtDecoder delegationJwtDecoder,
      ReactiveOAuth2AuthorizedClientService authorizedClientService) {

    http
        // ── CSRF ──────────────────────────────────────────────────────
        // Cookie-based CSRF: the SPA reads XSRF-TOKEN and sends it back
        // as X-XSRF-TOKEN header.  XOR-masked to prevent BREACH attacks.
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                    .requireCsrfProtectionMatcher(csrfProtectionMatcher())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))

        // ── Authorization ─────────────────────────────────────────────
        .authorizeExchange(
            auth ->
                auth.pathMatchers(PUBLIC_PATHS)
                    .permitAll()
                    .pathMatchers(HttpMethod.OPTIONS)
                    .permitAll() // pre-flight
                    .anyExchange()
                    .authenticated())

        // ── OAuth2 Login (Authorization Code flow) ────────────────────
        .oauth2Login(
            oauth2 ->
                oauth2
                    // TokenRelay uses the service-based manager below. Configure login to save
                    // authorized clients to the same store so idle-time refresh can find them.
                    .authorizedClientService(authorizedClientService)
                    // After successful login, redirect back to the SPA root (nginx will serve it)
                    .authenticationSuccessHandler(
                        new SpaAuthenticationSuccessHandler(postLogoutRedirectUri))
                    // Avoid default /login?error redirect (no server-side login page in SPA setup).
                    .authenticationFailureHandler(
                        new SpaAuthenticationFailureHandler(postLogoutRedirectUri)))

        // ── Bearer auth for delegation tokens ─────────────────────────
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(delegationJwtDecoder)))

        // ── Logout ────────────────────────────────────────────────────
        .logout(
            logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)))

        // ── Exception handling ────────────────────────────────────────
        // Return 401 for unauthenticated API calls instead of 302 redirect.
        // The SPA can intercept 401 and send the user to /oauth2/authorization/...
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))

        // ── Headers ───────────────────────────────────────────────────
        .headers(
            headers ->
                headers
                    .frameOptions(FrameOptionsSpec::disable)
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self'; "
                                    + "style-src 'self' 'unsafe-inline'; "
                                    + "img-src 'self' data:; "
                                    + "connect-src 'self'; "
                                    + "frame-ancestors 'none';")));

    return http.build();
  }

  private static ServerWebExchangeMatcher csrfProtectionMatcher() {
    return exchange -> {
      var authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        return ServerWebExchangeMatcher.MatchResult.notMatch();
      }
      return CsrfWebFilter.DEFAULT_CSRF_MATCHER.matches(exchange);
    };
  }

  /**
   * CSRF token subscription filter.
   *
   * <p>Spring Security's reactive CSRF support requires the token to be subscribed to, otherwise
   * the cookie is never written. This filter eagerly subscribes so every response carries the fresh
   * cookie.
   */
  @Bean
  public WebFilter csrfCookieWebFilter() {
    return (exchange, chain) -> {
      Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
      if (csrfToken != null) {
        return csrfToken
            .doOnSuccess(
                token -> {
                  // side-effect: Spring writes the cookie
                })
            .then(chain.filter(exchange));
      }
      return chain.filter(exchange);
    };
  }

  // ── Private helpers ───────────────────────────────────────────────────

  private ServerLogoutSuccessHandler oidcLogoutSuccessHandler(
      ReactiveClientRegistrationRepository repo) {

    var handler = new OidcClientInitiatedServerLogoutSuccessHandler(repo);
    handler.setPostLogoutRedirectUri(postLogoutRedirectUri);
    return handler;
  }

  // ── Inner classes ─────────────────────────────────────────────────────

  /**
   * Hybrid CSRF handler: uses XOR masking (BREACH-safe) for browser clients while also supporting
   * plain-token header submission for non-browser clients.
   */
  static final class SpaCsrfTokenRequestHandler extends ServerCsrfTokenRequestAttributeHandler {
    private final XorServerCsrfTokenRequestAttributeHandler xor =
        new XorServerCsrfTokenRequestAttributeHandler();
    private final ServerCsrfTokenRequestAttributeHandler plain =
        new ServerCsrfTokenRequestAttributeHandler();

    @Override
    public Mono<String> resolveCsrfTokenValue(ServerWebExchange exchange, CsrfToken csrfToken) {
      String headerValue = exchange.getRequest().getHeaders().getFirst(csrfToken.getHeaderName());
      if (headerValue != null && !headerValue.isEmpty()) {
        return plain.resolveCsrfTokenValue(exchange, csrfToken);
      }
      return xor.resolveCsrfTokenValue(exchange, csrfToken);
    }
  }
}
