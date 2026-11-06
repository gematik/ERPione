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

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import lombok.val;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class DelegationRelayGatewayFilter implements GlobalFilter, Ordered {

  private final DelegationRelayTokenStore relayTokenStore;

  public DelegationRelayGatewayFilter(DelegationRelayTokenStore relayTokenStore) {
    this.relayTokenStore = relayTokenStore;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    val path = exchange.getRequest().getPath().value();
    if (!path.startsWith("/bff/") || "/bff/delegations".equals(path) || "/bff/user".equals(path)) {
      return chain.filter(exchange);
    }

    val authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return chain.filter(exchange);
    }

    val delegationToken = authHeader.substring("Bearer ".length()).trim();
    if (delegationToken.isEmpty()) {
      return chain.filter(exchange);
    }

    val relayAccess = relayTokenStore.resolve(delegationToken);
    if (relayAccess == null) {
      return Mono.error(
          new ResponseStatusException(
              UNAUTHORIZED, "delegation token has no active relay access token"));
    }

    val mutatedRequest =
        exchange
            .getRequest()
            .mutate()
            .headers(headers -> headers.setBearerAuth(relayAccess.tokenValue()))
            .build();
    return chain.filter(exchange.mutate().request(mutatedRequest).build());
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
