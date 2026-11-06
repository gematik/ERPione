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
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.filter.headers.RemoveXForwardedHeadersFilter;
import org.springframework.cloud.gateway.filter.headers.XForwardedHeadersFilter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

/**
 * Executable specification for {@link GatewayConfig}.
 *
 * <p>Each test documents one routing rule or header-filter behaviour:
 *
 * <ul>
 *   <li>Which paths are handled <em>locally</em> by the BFF (not forwarded)
 *   <li>Which paths are forwarded to the <em>resource server</em>
 *   <li>That BFF-local endpoints have <em>priority</em> over the generic {@code /bff/**} proxy
 *   <li>How {@code trusted-proxies} decides between {@link XForwardedHeadersFilter} and {@link
 *       RemoveXForwardedHeadersFilter}
 * </ul>
 *
 * <p>This test uses {@link ReactiveWebApplicationContextRunner} to build a <em>minimal</em> context
 * containing only {@link GatewayAutoConfiguration} and {@link GatewayConfig}. Neither OAuth2 nor
 * Redis nor session infrastructure is started, so the test is unaffected by auto-configuration
 * reshuffling in Spring Boot / Spring Cloud upgrades.
 *
 * <p>Route matching is verified using {@link MockServerWebExchange} to simulate HTTP requests and
 * checking which route's predicate matches. No actual HTTP connections are made; no external
 * services need to be running.
 */
class GatewayConfigTest {

  private static final String TRUSTED_PROXIES =
      "spring.cloud.gateway.server.webflux.trusted-proxies";

  private final ReactiveWebApplicationContextRunner runner =
      new ReactiveWebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  SslAutoConfiguration.class,
                  WebFluxAutoConfiguration.class,
                  GatewayAutoConfiguration.class))
          .withUserConfiguration(RateLimiterStubConfig.class, GatewayConfig.class)
          .withPropertyValues(
              // Use a recognisable test hostname so URI assertions clearly show the destination
              "bff.resource-server.uri=http://resource-server-test:8081",
              // Non-empty trusted proxies keep XForwardedHeadersFilter active (production default)
              TRUSTED_PROXIES + "=127\\..*|10\\..*");

  // ── BFF-local endpoints ────────────────────────────────────────────────────
  //
  // These paths are served by the BFF itself (internal Spring WebFlux handlers).
  // They must NOT be forwarded to the resource server.

  @Test
  void apiUserProfile_isHandledByTheBff_notForwardedToResourceServer() {
    // GET /api/user returns the authenticated user's profile from the BFF session.
    // It is served by OAuth2UserController via forward:/internal/user.
    withGateway(
        ctx -> assertThat(routeIdFor(ctx, "GET", "/api/user")).isEqualTo("api-user-profile"));
  }

  @Test
  void bffUserProfile_isHandledByTheBff_notForwardedToResourceServer() {
    // GET /bff/user returns the authenticated user's profile from the BFF session.
    // It is served by OAuth2UserController via forward:/internal/user.
    withGateway(
        ctx -> assertThat(routeIdFor(ctx, "GET", "/bff/user")).isEqualTo("bff-user-profile"));
  }

  @Test
  void bffDelegations_isHandledByTheBff_notForwardedToResourceServer() {
    // GET /bff/delegations manages delegation tokens issued by the BFF.
    // It is served by DelegationController via forward:/internal/delegations.
    withGateway(
        ctx -> assertThat(routeIdFor(ctx, "GET", "/bff/delegations")).isEqualTo("bff-delegations"));
  }

  // ── /api/** → Resource Server (token relay) ───────────────────────────────
  //
  // The BFF injects the OAuth2 ****** from the session before forwarding.
  // The /api prefix is stripped, so /api/prescriptions → /prescriptions on the resource server.

  @Test
  void apiRequests_areForwardedToResourceServer() {
    withGateway(
        ctx -> {
          assertThat(routeIdFor(ctx, "GET", "/api/prescriptions")).isEqualTo("resource-server");
          assertThat(routeIdFor(ctx, "GET", "/api/prescriptions/abc123"))
              .isEqualTo("resource-server");
          assertThat(routeIdFor(ctx, "POST", "/api/prescriptions")).isEqualTo("resource-server");
          assertThat(routeIdFor(ctx, "DELETE", "/api/prescriptions/abc123"))
              .isEqualTo("resource-server");
        });
  }

  @Test
  void apiRequests_areForwardedToConfiguredResourceServerHost() {
    withGateway(
        ctx ->
            assertThat(routeUriFor(ctx, "GET", "/api/prescriptions"))
                .hasHost("resource-server-test")
                .hasPort(8081));
  }

  // ── /bff/** → Resource Server (path rewrite + token relay) ───────────────
  //
  // The SPA sends requests to /bff/**. The BFF rewrites the path before forwarding:
  //   /bff/prescriptions/abc  →  /api/prescriptions/abc  on the resource server
  // This decouples the SPA from the internal /api/** structure.

  @Test
  void bffPrefixedRequests_areForwardedToResourceServer_withPathRewrite() {
    withGateway(
        ctx -> {
          assertThat(routeIdFor(ctx, "GET", "/bff/prescriptions")).isEqualTo("resource-server-bff");
          assertThat(routeIdFor(ctx, "POST", "/bff/prescriptions"))
              .isEqualTo("resource-server-bff");
          assertThat(routeIdFor(ctx, "GET", "/bff/prescriptions/abc123"))
              .isEqualTo("resource-server-bff");
        });
  }

  @Test
  void bffPrefixedRequests_areForwardedToConfiguredResourceServerHost() {
    withGateway(
        ctx ->
            assertThat(routeUriFor(ctx, "GET", "/bff/prescriptions"))
                .hasHost("resource-server-test")
                .hasPort(8081));
  }

  // ── Route priority ─────────────────────────────────────────────────────────
  //
  // /api/user and /bff/user are registered with order(-100) to ensure they are matched
  // BEFORE the generic /api/** and /bff/** proxy routes. Without the higher priority,
  // those paths would be intercepted by the proxy and incorrectly forwarded to the
  // resource server instead of being served locally.

  @Test
  void userProfileEndpoints_takePriorityOverGeneralProxyRoutes() {
    withGateway(
        ctx -> {
          assertThat(routeIdFor(ctx, "GET", "/api/user")).isNotEqualTo("resource-server");
          assertThat(routeIdFor(ctx, "GET", "/bff/user")).isNotEqualTo("resource-server-bff");
        });
  }

  @Test
  void bffLocalEndpoints_takePriorityOverGeneralBffProxyRoute() {
    withGateway(
        ctx ->
            assertThat(routeIdFor(ctx, "GET", "/bff/delegations"))
                .isNotEqualTo("resource-server-bff"));
  }

  // ── X-Forwarded-* header handling ─────────────────────────────────────────

  @Test
  void xForwardedHeaders_areReinjectedFromReconstructedUri_forTrustedProxyRequests() {
    withGateway(
        ctx -> {
          // Verify the active filters: XForwardedHeadersFilter must be present (not
          // RemoveXForwardedHeadersFilter, which would strip all X-Forwarded-* headers).
          assertThat(httpHeadersFilters(ctx))
              .anyMatch(XForwardedHeadersFilter.class::isInstance)
              .noneMatch(RemoveXForwardedHeadersFilter.class::isInstance);

          // Simulate the exchange state *after* NATIVE ForwardedHeaderTransformer has run:
          //  – URI reconstructed to https://example.external (from nginx's X-Forwarded-*)
          //  – Host header preserved as set by nginx (proxy_set_header Host $host)
          //  – X-Forwarded-Host, X-Forwarded-Proto, X-Forwarded-Port removed (consumed by NATIVE)
          //  – X-Forwarded-For retained for auditing (not consumed by NATIVE)
          val exchange =
              MockServerWebExchange.from(
                  MockServerHttpRequest.get("https://example.external/api/prescriptions")
                      .remoteAddress(new InetSocketAddress("10.100.1.7", 12345))
                      .header("Host", "example.external")
                      .header("X-Forwarded-For", "198.51.100.10")
                      .build());

          val orderedFilters = new ArrayList<>(httpHeadersFilters(ctx));
          AnnotationAwareOrderComparator.sort(orderedFilters);
          val filteredHeaders = HttpHeadersFilter.filterRequest(orderedFilters, exchange);

          // XForwardedHeadersFilter must re-inject the external values from the reconstructed URI
          // so the downstream resource server (and springdoc) sees the correct public URL.
          assertThat(filteredHeaders.getFirst("X-Forwarded-Proto")).isEqualTo("https");
          assertThat(filteredHeaders.getFirst("X-Forwarded-Host")).isEqualTo("example.external");
          assertThat(filteredHeaders.getFirst("X-Forwarded-Port")).isEqualTo("443");
          assertThat(filteredHeaders.getFirst("X-Forwarded-For")).contains("10.100.1.7");
        });
  }

  @Test
  void emptyTrustedProxies_activatesRemoveXForwardedHeadersFilter() {
    // Regression guard: if trusted-proxies resolves to an empty value, Gateway swaps
    // XForwardedHeadersFilter for RemoveXForwardedHeadersFilter and strips all X-Forwarded-*
    // headers. ForwardedHeadersStartupGuard exists to fail fast on exactly this situation.
    runner
        .withPropertyValues(TRUSTED_PROXIES + "=")
        .run(
            ctx ->
                assertThat(httpHeadersFilters(ctx))
                    .anyMatch(RemoveXForwardedHeadersFilter.class::isInstance)
                    .noneMatch(XForwardedHeadersFilter.class::isInstance));
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private void withGateway(Consumer<AssertableReactiveWebApplicationContext> assertions) {
    runner.run(assertions::accept);
  }

  private List<HttpHeadersFilter> httpHeadersFilters(
      AssertableReactiveWebApplicationContext context) {
    return List.copyOf(context.getBeansOfType(HttpHeadersFilter.class).values());
  }

  private String routeIdFor(
      AssertableReactiveWebApplicationContext context, String method, String path) {
    return firstMatchingRoute(context, method, path).getId();
  }

  private URI routeUriFor(
      AssertableReactiveWebApplicationContext context, String method, String path) {
    return firstMatchingRoute(context, method, path).getUri();
  }

  private Route firstMatchingRoute(
      AssertableReactiveWebApplicationContext context, String method, String path) {
    val exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.method(HttpMethod.valueOf(method), URI.create(path)).build());
    return context
        .getBean(RouteLocator.class)
        .getRoutes()
        .filterWhen(route -> route.getPredicate().apply(exchange))
        .blockFirst();
  }

  /**
   * {@link GatewayConfig#redisRateLimiter()} resolves a {@link ReactiveStringRedisTemplate} and the
   * rate-limiter Lua script from the context on startup. Stubbing both keeps Redis out of this
   * test; the rate limiter is never invoked because no request is actually proxied.
   */
  @Configuration(proxyBeanMethods = false)
  static class RateLimiterStubConfig {

    @Bean
    ReactiveStringRedisTemplate reactiveStringRedisTemplate() {
      return mock(ReactiveStringRedisTemplate.class);
    }

    @Bean(name = RedisRateLimiter.REDIS_SCRIPT_NAME)
    @SuppressWarnings("unchecked")
    RedisScript<List<Long>> redisRequestRateLimiterScript() {
      return mock(RedisScript.class);
    }
  }
}
