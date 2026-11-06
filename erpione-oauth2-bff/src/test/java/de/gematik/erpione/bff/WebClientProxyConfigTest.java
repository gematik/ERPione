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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.channel.ChannelOption;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.HttpClientFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

class WebClientProxyConfigTest {

  @Test
  void sharedHttpClient_hasConnectTimeoutButNoResponseTimeout_soGatewayRouteTimeoutStillApplies() {
    var config = new WebClientProxyConfig();
    var provider = config.outboundConnectionProvider();
    try {
      var httpClient = config.proxyAwareHttpClient(provider);

      assertThat(httpClient.configuration().responseTimeout()).isNull();
      assertThat(httpClient.configuration().options().get(ChannelOption.CONNECT_TIMEOUT_MILLIS))
          .isEqualTo(WebClientProxyConfig.OUTBOUND_CONNECT_TIMEOUT_MILLIS);
      assertThat(httpClient.configuration().connectionProvider()).isSameAs(provider);
    } finally {
      provider.dispose();
    }
  }

  @Test
  void idpHttpClient_hasBoundedResponseTimeout_withoutChangingSharedClient() {
    var sharedClient = HttpClient.create();

    var idpClient = WebClientProxyConfig.withIdpResponseTimeout(sharedClient);

    assertThat(idpClient.configuration().responseTimeout())
        .isEqualTo(WebClientProxyConfig.IDP_RESPONSE_TIMEOUT);
    assertThat(sharedClient.configuration().responseTimeout()).isNull();
  }

  @Test
  void idpCallLogging_passesResponseThrough() {
    var request =
        ClientRequest.create(HttpMethod.POST, URI.create("https://idp.example/realms/x/token"))
            .build();
    var response = ClientResponse.create(HttpStatus.OK).build();
    ExchangeFunction next = r -> Mono.just(response);

    StepVerifier.create(WebClientProxyConfig.idpCallLogging().filter(request, next))
        .expectNext(response)
        .verifyComplete();
  }

  @Test
  void idpCallLogging_propagatesFailure() {
    var request =
        ClientRequest.create(HttpMethod.POST, URI.create("https://idp.example/realms/x/token"))
            .build();
    var failure = new IllegalStateException("read timeout");
    ExchangeFunction next = r -> Mono.error(failure);

    StepVerifier.create(WebClientProxyConfig.idpCallLogging().filter(request, next))
        .expectErrorMatches(error -> error == failure)
        .verify();
  }

  @Test
  void gatewayRoutingUsesTheSharedHttpClientBean() {
    // Guards the assumption behind the idle-connection fix: when an HttpClient bean exists,
    // Spring Cloud Gateway does not create its own, so this pool also serves /api/** routes.
    new ReactiveWebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                SslAutoConfiguration.class,
                WebFluxAutoConfiguration.class,
                GatewayAutoConfiguration.class))
        .withBean(HttpClient.class, HttpClient::create)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBeansOfType(HttpClient.class)).hasSize(1);
              assertThat(context).doesNotHaveBean(HttpClientFactory.class);
            });
  }

  @Test
  void managerLoadsAuthorizedClientThroughTheConfiguredService() {
    var registration = registration();
    var principal = new TestingAuthenticationToken("user", "credentials");
    var client = authorizedClient(registration);
    var registrations = mock(ReactiveClientRegistrationRepository.class);
    var service = mock(ReactiveOAuth2AuthorizedClientService.class);
    var proxyWebClient = mock(WebClient.class);

    doReturn(Mono.just(registration)).when(registrations).findByRegistrationId("oidc");
    doReturn(Mono.just(client)).when(service).loadAuthorizedClient("oidc", principal.getName());
    var manager =
        new WebClientProxyConfig()
            .authorizedClientManager(registrations, service, 60, proxyWebClient);

    var result =
        manager
            .authorize(
                OAuth2AuthorizeRequest.withClientRegistrationId("oidc")
                    .principal(principal)
                    .build())
            .block();

    assertThat(result).isSameAs(client);
  }

  private static ClientRegistration registration() {
    return ClientRegistration.withRegistrationId("oidc")
        .clientId("client")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .authorizationUri("https://idp.example/authorize")
        .tokenUri("https://idp.example/token")
        .build();
  }

  private static OAuth2AuthorizedClient authorizedClient(ClientRegistration registration) {
    var now = Instant.now();
    var accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            now.minusSeconds(30),
            now.plusSeconds(300),
            Set.of("openid"));
    return new OAuth2AuthorizedClient(registration, "user", accessToken);
  }
}
