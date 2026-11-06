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

package de.gematik.erpione.server.oauth;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class OAuth2LoginConfig {

  @Bean
  public WebSecurityCustomizer swaggerSecurityCustomizer() {
    return web ->
        web.ignoring().requestMatchers("/v3/api-docs/**", "/api-docs/**", "/swagger-ui/**");
  }

  @Bean
  @SneakyThrows
  public SecurityFilterChain api(HttpSecurity http, Environment environment) {
    // no OAuth2 login if "noauth" profile is active
    // this profile can be used in environment where no auth is required
    val noauth = isNoAuthProfileActive(environment);

    if (!noauth) {
      return authFilterChain(http);
    } else {
      return noauthFilterChain(http);
    }
  }

  private static boolean isNoAuthProfileActive(Environment environment) {
    return Arrays.asList(environment.getActiveProfiles()).contains("noauth");
  }

  private SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
    http.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());
    return http.build();
  }

  /** Disables CSRF protection for noauth mode */
  @SuppressWarnings("java:S4502")
  private SecurityFilterChain noauthFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authorizeRequests -> authorizeRequests.anyRequest().permitAll());
    return http.build();
  }
}
