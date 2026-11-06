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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.cloud.gateway.filter.headers.RemoveXForwardedHeadersFilter;
import org.springframework.cloud.gateway.filter.headers.XForwardedHeadersFilter;
import org.springframework.mock.env.MockEnvironment;

class ForwardedHeadersStartupGuardTest {

  @Test
  void run_failsWhenTrustedProxiesResolvesEmpty() {
    val env =
        new MockEnvironment()
            .withProperty("spring.cloud.gateway.server.webflux.trusted-proxies", "")
            .withProperty("bff.forwarded-headers.fail-on-empty-trusted-proxies", "true")
            .withProperty("bff.forwarded-headers.fail-on-remove-filter", "false");

    val guard =
        new ForwardedHeadersStartupGuard(env, List.of(new XForwardedHeadersFilter("127\\..*")));

    val appArgs = new DefaultApplicationArguments();
    assertThatThrownBy(() -> guard.run(appArgs))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("trusted-proxies resolved to empty");
  }

  @Test
  void run_failsWhenRemoveXForwardedHeadersFilterIsActive() {
    val env =
        new MockEnvironment()
            .withProperty("spring.cloud.gateway.server.webflux.trusted-proxies", "127\\..*")
            .withProperty("bff.forwarded-headers.fail-on-empty-trusted-proxies", "true")
            .withProperty("bff.forwarded-headers.fail-on-remove-filter", "true");

    val guard = new ForwardedHeadersStartupGuard(env, List.of(new RemoveXForwardedHeadersFilter()));

    val appArgs = new DefaultApplicationArguments();
    assertThatThrownBy(() -> guard.run(appArgs))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("RemoveXForwardedHeadersFilter is active");
  }

  @Test
  void run_allowsValidTrustedProxySetup() {
    val env =
        new MockEnvironment()
            .withProperty("spring.cloud.gateway.server.webflux.trusted-proxies", "127\\..*")
            .withProperty("bff.forwarded-headers.fail-on-empty-trusted-proxies", "true")
            .withProperty("bff.forwarded-headers.fail-on-remove-filter", "true");

    val guard =
        new ForwardedHeadersStartupGuard(env, List.of(new XForwardedHeadersFilter("127\\..*")));

    val appArgs = new DefaultApplicationArguments();
    assertThatCode(() -> guard.run(appArgs)).doesNotThrowAnyException();
  }
}
