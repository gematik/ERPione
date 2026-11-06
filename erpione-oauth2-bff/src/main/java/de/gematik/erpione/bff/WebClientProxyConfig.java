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

import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.RefreshTokenReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.oidc.authentication.ReactiveOidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoderFactory;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

@Slf4j
@Configuration
public class WebClientProxyConfig {

  static final Duration OUTBOUND_MAX_IDLE_TIME = Duration.ofSeconds(30);
  static final Duration OUTBOUND_MAX_LIFE_TIME = Duration.ofMinutes(5);
  static final Duration OUTBOUND_EVICTION_INTERVAL = Duration.ofSeconds(30);
  static final int OUTBOUND_CONNECT_TIMEOUT_MILLIS = 5_000;
  static final Duration IDP_RESPONSE_TIMEOUT = Duration.ofSeconds(10);

  @Value("${PROXY_HOST:}")
  private String proxyHost;

  @Value("${PROXY_PORT:3128}")
  private int proxyPort;

  @Value("${NO_PROXY:}")
  private String noProxy;

  @PostConstruct
  public void logProxySettings() {
    log.info(
        "Proxy environment: PROXY_HOST='{}', PROXY_PORT='{}', NO_PROXY='{}'",
        proxyHost,
        proxyPort,
        noProxy);
    log.info(
        "Proxy system properties: http.proxyHost='{}', https.proxyHost='{}'",
        System.getProperty("http.proxyHost", "<unset>"),
        System.getProperty("https.proxyHost", "<unset>"));
    var h = resolveProxyHost();
    log.info(
        "Effective proxy: host='{}', port={}",
        h != null ? h : "<none - direct connections>",
        h != null ? resolveProxyPort() : "-");
  }

  /**
   * Connection pool for all outbound calls: IdP calls and, because Spring Cloud Gateway skips its
   * own HttpClient when one is defined, also the proxied resource-server routes.
   *
   * <p>Idle connections are evicted before intermediaries (proxy, NAT, load balancer) silently drop
   * them. Otherwise, the first request after a pause can be written to a dead connection and never
   * receive a response.
   */
  @Bean(destroyMethod = "dispose")
  public ConnectionProvider outboundConnectionProvider() {
    return ConnectionProvider.builder("bff-outbound")
        .maxIdleTime(OUTBOUND_MAX_IDLE_TIME)
        .maxLifeTime(OUTBOUND_MAX_LIFE_TIME)
        .evictInBackground(OUTBOUND_EVICTION_INTERVAL)
        .build();
  }

  /**
   * Shared HttpClient. It is also used by the gateway routing filter, which applies its own
   * route/global response timeout, so no response timeout is configured here.
   */
  @Bean
  public HttpClient proxyAwareHttpClient(ConnectionProvider outboundConnectionProvider) {
    var baseClient =
        HttpClient.create(outboundConnectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, OUTBOUND_CONNECT_TIMEOUT_MILLIS);
    var effectiveHost = resolveProxyHost();
    if (effectiveHost != null) {
      var effectivePort = resolveProxyPort();
      var nonProxyHostsPattern = toNonProxyHostsPattern(effectiveNoProxyList());
      log.info("Configuring outbound proxy {}:{}", effectiveHost, effectivePort);
      final String host = effectiveHost;
      final int port = effectivePort;
      return baseClient.proxy(
          typeSpec -> {
            var builder = typeSpec.type(ProxyProvider.Proxy.HTTP).host(host).port(port);
            if (nonProxyHostsPattern != null) {
              builder.nonProxyHosts(nonProxyHostsPattern);
            }
          });
    }
    log.warn(
        "No proxy configured (PROXY_HOST and http[s].proxyHost are all blank). "
            + "Outbound connections will be direct.");
    return baseClient;
  }

  /** Resolves the effective proxy host from env var or JVM system properties (in that order). */
  private String resolveProxyHost() {
    if (proxyHost != null && !proxyHost.isBlank()) {
      return normalizeProxyHost(proxyHost);
    }
    var h = System.getProperty("https.proxyHost", "").trim();
    if (!h.isBlank()) return h;
    h = System.getProperty("http.proxyHost", "").trim();
    if (!h.isBlank()) return h;
    return null;
  }

  /**
   * Resolves the effective proxy port. Tries env/field first, then system properties matching the
   * chosen host property key.
   */
  private int resolveProxyPort() {
    if (proxyHost != null && !proxyHost.isBlank()) {
      return proxyPort;
    }
    if (!System.getProperty("https.proxyHost", "").isBlank()) {
      try {
        return Integer.parseInt(System.getProperty("https.proxyPort", String.valueOf(proxyPort)));
      } catch (NumberFormatException ignored) {
        // fall through
      }
    }
    try {
      return Integer.parseInt(System.getProperty("http.proxyPort", String.valueOf(proxyPort)));
    } catch (NumberFormatException ignored) {
      return proxyPort;
    }
  }

  /** Returns the non-proxy list from env var or JVM system property. */
  private String effectiveNoProxyList() {
    if (noProxy != null && !noProxy.isBlank()) {
      return noProxy;
    }
    return System.getProperty("http.nonProxyHosts", "");
  }

  /**
   * Connector for IdP calls (token, refresh, userinfo, JWKS). A hanging IdP call would otherwise
   * block TokenRelay indefinitely, so it gets a bounded response timeout.
   */
  @Bean
  public ReactorClientHttpConnector proxyClientHttpConnector(HttpClient proxyAwareHttpClient) {
    return new ReactorClientHttpConnector(withIdpResponseTimeout(proxyAwareHttpClient));
  }

  static HttpClient withIdpResponseTimeout(HttpClient httpClient) {
    return httpClient.responseTimeout(IDP_RESPONSE_TIMEOUT);
  }

  @Bean("proxyWebClient")
  public WebClient proxyWebClient(ReactorClientHttpConnector proxyClientHttpConnector) {
    return WebClient.builder()
        .clientConnector(proxyClientHttpConnector)
        .filter(idpCallLogging())
        .build();
  }

  /** Logs every IdP call without query strings, headers, bodies, or token values. */
  static ExchangeFilterFunction idpCallLogging() {
    return (request, next) -> {
      var host = request.url().getHost();
      var path = request.url().getPath();
      var startNanos = System.nanoTime();
      log.info("IdP call started: method={}, host={}, path={}", request.method(), host, path);
      return next.exchange(request)
          .doOnNext(
              response ->
                  log.info(
                      "IdP call completed: method={}, host={}, path={}, status={}, durationMs={}",
                      request.method(),
                      host,
                      path,
                      response.statusCode().value(),
                      elapsedMillis(startNanos)))
          .doOnError(
              error ->
                  log.warn(
                      "IdP call failed: method={}, host={}, path={}, error={}, cause={},"
                          + " durationMs={}",
                      request.method(),
                      host,
                      path,
                      error.getClass().getSimpleName(),
                      error.getCause() == null ? null : error.getCause().getClass().getSimpleName(),
                      elapsedMillis(startNanos)))
          .doOnCancel(
              () ->
                  log.warn(
                      "IdP call cancelled: method={}, host={}, path={}, durationMs={}",
                      request.method(),
                      host,
                      path,
                      elapsedMillis(startNanos)));
    };
  }

  private static long elapsedMillis(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
  }

  /** Overrides Spring Security's default OAuth2 UserInfo client with a proxy-aware WebClient. */
  @Bean
  public ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(
      @Qualifier("proxyWebClient") WebClient proxyWebClient) {
    var userService = new DefaultReactiveOAuth2UserService();
    userService.setWebClient(proxyWebClient);
    return userService;
  }

  /**
   * Overrides Spring Security's default OIDC UserInfo service and delegates UserInfo calls to the
   * proxy-aware OAuth2 user service.
   */
  @Bean
  public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(
      ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService) {
    var oidcUserService = new OidcReactiveOAuth2UserService();
    oidcUserService.setOauth2UserService(oauth2UserService);
    return oidcUserService;
  }

  /**
   * Authorized client manager used by Spring Cloud Gateway's TokenRelay filter and the general
   * application WebClient. Explicitly uses a proxy-aware WebClient for token refresh calls so that
   * access-token renewals also go through the proxy.
   */
  @Bean
  public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
      ReactiveClientRegistrationRepository clientRegistrationRepository,
      ReactiveOAuth2AuthorizedClientService authorizedClientService,
      @Value("${bff.delegation.refresh-before-expiry-seconds:60}") long refreshBeforeExpirySeconds,
      @Qualifier("proxyWebClient") WebClient proxyWebClient) {

    var refreshTokenClient = new WebClientReactiveRefreshTokenTokenResponseClient();
    refreshTokenClient.setWebClient(proxyWebClient);

    var refreshProvider = new RefreshTokenReactiveOAuth2AuthorizedClientProvider();
    refreshProvider.setAccessTokenResponseClient(refreshTokenClient);

    var authorizedClientProvider =
        ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
            .authorizationCode()
            .refreshToken(
                cfg ->
                    cfg.accessTokenResponseClient(refreshTokenClient)
                        .clockSkew(Duration.ofSeconds(refreshBeforeExpirySeconds)))
            .clientCredentials()
            .build();

    var manager =
        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService);
    manager.setAuthorizedClientProvider(authorizedClientProvider);
    manager.setAuthorizationSuccessHandler(
        (authorizedClient, principal, attributes) -> {
          var expiresAt = authorizedClient.getAccessToken().getExpiresAt();
          log.info(
              "OAuth2 authorization succeeded: registration={}, principalType={},"
                  + " accessTokenExpiresAt={}, expiresInSeconds={}",
              authorizedClient.getClientRegistration().getRegistrationId(),
              principal.getClass().getSimpleName(),
              expiresAt,
              expiresAt == null
                  ? null
                  : Duration.between(java.time.Instant.now(), expiresAt).toSeconds());
          return authorizedClientService.saveAuthorizedClient(authorizedClient, principal);
        });
    var removeStaleClientHandler =
        new RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler(
            (clientRegistrationId, principal, attributes) ->
                authorizedClientService.removeAuthorizedClient(
                    clientRegistrationId, principal.getName()));
    manager.setAuthorizationFailureHandler(
        (authorizationException, principal, attributes) -> {
          log.warn(
              "OAuth2 authorization failed: errorCode={}, principalType={}, registration={}",
              authorizationException.getError().getErrorCode(),
              principal == null ? null : principal.getClass().getSimpleName(),
              authorizationException instanceof ClientAuthorizationException clientException
                  ? clientException.getClientRegistrationId()
                  : null);
          return removeStaleClientHandler.onAuthorizationFailure(
              authorizationException, principal, attributes);
        });
    return manager;
  }

  @Bean
  public WebClient webClient(
      ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
      ReactorClientHttpConnector proxyClientHttpConnector) {
    var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    return WebClient.builder().filter(oauth).clientConnector(proxyClientHttpConnector).build();
  }

  /**
   * Exposes the proxy-aware authorization-code token response client as a Spring bean so that
   * Spring Security's {@code OAuth2LoginSpec.createDefault()} auto-detects it via {@code
   * getBeanOrNull(ResolvableType.forClassWithGenerics(...))} and wires it into both the {@link
   * org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeReactiveAuthenticationManager}
   * and {@link
   * org.springframework.security.oauth2.client.authentication.OAuth2LoginReactiveAuthenticationManager}
   * that it builds internally — routing the token-exchange POST through the proxy.
   */
  @Bean
  public ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
      authorizationCodeTokenResponseClient(@Qualifier("proxyWebClient") WebClient proxyWebClient) {
    var tokenResponseClient = new WebClientReactiveAuthorizationCodeTokenResponseClient();
    tokenResponseClient.setWebClient(proxyWebClient);
    return tokenResponseClient;
  }

  /**
   * Ensures OIDC ID-token signature key retrieval (JWKS) also goes through the outbound proxy.
   *
   * <p>Without this bean, Spring Security's default {@code ReactiveOidcIdTokenDecoderFactory}
   * builds decoders with a default WebClient that does not use our proxy connector.
   */
  @Bean
  public ReactiveJwtDecoderFactory<ClientRegistration> oidcJwtDecoderFactory(
      @Qualifier("proxyWebClient") WebClient proxyWebClient) {
    var claimTypeConverter =
        new ClaimTypeConverter(
            ReactiveOidcIdTokenDecoderFactory.createDefaultClaimTypeConverters());
    Map<String, ReactiveJwtDecoder> decoders = new ConcurrentHashMap<>();

    return clientRegistration ->
        decoders.computeIfAbsent(
            clientRegistration.getRegistrationId(),
            registrationId -> {
              var jwkSetUri = clientRegistration.getProviderDetails().getJwkSetUri();
              var decoder =
                  NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                      .webClient(proxyWebClient)
                      .build();
              decoder.setJwtValidator(
                  JwtValidators.createDefaultWithValidators(
                      new OidcIdTokenValidator(clientRegistration)));
              decoder.setClaimSetConverter(claimTypeConverter);
              return decoder;
            });
  }

  private static String normalizeProxyHost(String host) {
    var normalized = host;
    if (normalized.startsWith("http://")) {
      normalized = normalized.substring("http://".length());
    } else if (normalized.startsWith("https://")) {
      normalized = normalized.substring("https://".length());
    }
    if (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private static String toNonProxyHostsPattern(String configuredNoProxy) {
    if (configuredNoProxy == null || configuredNoProxy.isBlank()) {
      return null;
    }
    return java.util.Arrays.stream(configuredNoProxy.split("[,|\\s]+"))
        .map(String::trim)
        .filter(entry -> !entry.isEmpty())
        .map(WebClientProxyConfig::toRegexFragment)
        .reduce((left, right) -> left + "|" + right)
        .orElse(null);
  }

  private static String toRegexFragment(String pattern) {
    StringBuilder regex = new StringBuilder(pattern.length() * 2);
    for (char c : pattern.toCharArray()) {
      if (c == '*') {
        regex.append(".*");
      } else if ("\\.[]{}()+-^$|?".indexOf(c) >= 0) {
        regex.append('\\').append(c);
      } else {
        regex.append(c);
      }
    }
    return regex.toString();
  }
}
