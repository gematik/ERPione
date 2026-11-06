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
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Spring Cloud Gateway route configuration.
 *
 * <p>The {@code TokenRelay} filter is the core of the BFF pattern: it extracts the OAuth2
 * access-token from the server-side session and injects it as a {@code Authorization: Bearer …}
 * header before forwarding the request to the resource-server. The browser never sees the token.
 *
 * <pre>
 *  Browser ──(session cookie)──► BFF/Gateway ──(Bearer token)──► Resource Server
 * </pre>
 *
 * <p>Routes defined here:
 *
 * <ul>
 *   <li>{@code /api/user}, {@code /bff/user} → local BFF profile endpoint
 *   <li>{@code /api/**} → Resource Server (token relay enabled)
 *   <li>{@code /bff/**} → rewritten to {@code /api/**} on Resource Server (token relay enabled)
 *   <li>{@code /swagger-ui/**}, {@code /api-docs/**}, {@code /v3/api-docs/**} → Resource Server
 *       (passthrough)
 *   <li>{@code /static/**} → optional static asset server
 * </ul>
 */
@Configuration
public class GatewayConfig {

  @Value("${bff.resource-server.uri:http://resource-server:8080}")
  private String resourceServerUri;

  @Value("${bff.strip-prefix-parts:1}")
  private int stripPrefixParts;

  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder
        .routes()

        // ── Route 0: user profile endpoints handled locally ─────────────
        .route(
            "api-user-profile", r -> r.order(-100).path("/api/user").uri("forward:/internal/user"))
        .route(
            "bff-user-profile", r -> r.order(-100).path("/bff/user").uri("forward:/internal/user"))
        .route(
            "bff-delegations",
            r -> r.order(-100).path("/bff/delegations").uri("forward:/internal/delegations"))

        // ── Route 1: API → Resource Server ──────────────────────────
        // All /api/** calls get the bearer token injected automatically.
        .route(
            "resource-server",
            r ->
                r.path("/api/**")
                    .filters(
                        f ->
                            f.tokenRelay()
                                // Add a custom header so the resource server can identify BFF
                                // traffic
                                .addRequestHeader("X-BFF-Client", "spring-bff")
                                // Remove sensitive headers coming back from resource server
                                .removeResponseHeader("Set-Cookie")
                                // Retry on 503
                                .retry(
                                    config ->
                                        config
                                            .setRetries(2)
                                            .setStatuses(
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT)
                                            .setMethods(HttpMethod.GET, HttpMethod.HEAD))
                                // Rate limiting (uses Redis via spring-session-data-redis)
                                .requestRateLimiter(
                                    rl ->
                                        rl.setRateLimiter(redisRateLimiter())
                                            .setKeyResolver(userKeyResolver())))
                    .uri(resourceServerUri))

        // ── Route 2: BFF facade → Resource Server API ───────────────────
        // /bff/a/b/c becomes /api/a/b/c on the resource server.
        .route(
            "resource-server-bff",
            r ->
                r.path("/bff/**")
                    .filters(
                        f ->
                            f.tokenRelay()
                                .stripPrefix(stripPrefixParts)
                                .prefixPath("/api")
                                .addRequestHeader("X-BFF-Client", "spring-bff")
                                .removeResponseHeader("Set-Cookie")
                                .retry(
                                    config ->
                                        config
                                            .setRetries(2)
                                            .setStatuses(
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT)
                                            .setMethods(HttpMethod.GET, HttpMethod.HEAD))
                                .requestRateLimiter(
                                    rl ->
                                        rl.setRateLimiter(redisRateLimiter())
                                            .setKeyResolver(userKeyResolver())))
                    .uri(resourceServerUri))
        .build();
  }

  // ── Rate limiter – token-bucket per authenticated user ────────────────

  @Bean
  public RedisRateLimiter redisRateLimiter() {
    // replenishRate=50 req/s, burstCapacity=100, requestedTokens=1
    return new RedisRateLimiter(50, 100, 1);
  }

  /**
   * Use the authenticated username as the rate-limit key. Falls back to remote IP for anonymous
   * (should not happen for /api/** or /bff/**).
   */
  @Bean
  public KeyResolver userKeyResolver() {
    return exchange ->
        exchange
            .getPrincipal()
            .map(java.security.Principal::getName)
            .defaultIfEmpty(
                exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getHostString()
                    : "anonymous");
  }
}
