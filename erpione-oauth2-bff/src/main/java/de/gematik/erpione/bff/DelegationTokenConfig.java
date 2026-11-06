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

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class DelegationTokenConfig {

  @Bean
  public JwtEncoder delegationJwtEncoder(
      @Value("${bff.delegation.secret:change-this-delegation-secret-with-at-least-32-characters}")
          String delegationSecret) {
    var key = createSecretKey(delegationSecret);
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  public ReactiveJwtDecoder delegationJwtDecoder(
      @Value("${bff.delegation.secret:change-this-delegation-secret-with-at-least-32-characters}")
          String delegationSecret) {
    var key = createSecretKey(delegationSecret);
    return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  private static SecretKey createSecretKey(String delegationSecret) {
    if (delegationSecret == null || delegationSecret.length() < 32) {
      throw new IllegalStateException(
          "Property bff.delegation.secret must be configured with at least 32 characters");
    }
    return new SecretKeySpec(delegationSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }
}
