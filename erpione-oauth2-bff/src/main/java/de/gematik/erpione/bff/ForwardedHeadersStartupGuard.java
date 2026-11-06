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

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.filter.headers.RemoveXForwardedHeadersFilter;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Startup validation/diagnostics to prevent silently stripped X-Forwarded headers. */
@Slf4j
@Component
public class ForwardedHeadersStartupGuard implements ApplicationRunner {

  private final Environment environment;
  private final List<HttpHeadersFilter> httpHeadersFilters;

  public ForwardedHeadersStartupGuard(
      Environment environment, List<HttpHeadersFilter> httpHeadersFilters) {
    this.environment = environment;
    this.httpHeadersFilters = httpHeadersFilters;
  }

  @Override
  public void run(ApplicationArguments args) {
    val trustedProxies =
        environment.getProperty("spring.cloud.gateway.server.webflux.trusted-proxies");
    val envTrustedProxies = environment.getProperty("GATEWAY_TRUSTED_PROXIES");

    val ordered = new ArrayList<>(httpHeadersFilters);
    AnnotationAwareOrderComparator.sort(ordered);
    val filterNames = ordered.stream().map(f -> f.getClass().getSimpleName()).toList();

    log.info(
        "Forwarded headers setup: trustedProxies='{}' env.GATEWAY_TRUSTED_PROXIES='{}'"
            + " requestHeaderFilters={}",
        trustedProxies,
        envTrustedProxies,
        filterNames);

    val failOnEmptyTrustedProxies =
        environment.getProperty(
            "bff.forwarded-headers.fail-on-empty-trusted-proxies", Boolean.class, true);
    if (failOnEmptyTrustedProxies && !StringUtils.hasText(trustedProxies)) {
      throw new IllegalStateException(
          "spring.cloud.gateway.server.webflux.trusted-proxies resolved to empty. "
              + "If GATEWAY_TRUSTED_PROXIES is set, it must not be blank.");
    }

    val failOnRemoveFilter =
        environment.getProperty("bff.forwarded-headers.fail-on-remove-filter", Boolean.class, true);
    if (failOnRemoveFilter
        && ordered.stream().anyMatch(RemoveXForwardedHeadersFilter.class::isInstance)) {
      throw new IllegalStateException(
          "RemoveXForwardedHeadersFilter is active. "
              + "Check trusted-proxies configuration to avoid stripping X-Forwarded-* headers.");
    }
  }
}
