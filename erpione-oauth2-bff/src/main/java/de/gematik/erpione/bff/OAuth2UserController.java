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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuth2UserController {

  @GetMapping("/internal/user")
  public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
    return Map.of(
        "picture",
        getPicture(principal),
        "name",
        getName(principal),
        "username",
        getUserName(principal));
  }

  private static String getPicture(OAuth2User principal) {
    return getAttribute(principal, "avatar_url", "picture");
  }

  private static String getName(OAuth2User principal) {
    return getAttribute(principal, "name", "given_name");
  }

  private static String getUserName(OAuth2User principal) {
    return getAttribute(principal, "email", "login", "preferred_username", "sub");
  }

  private static String getAttribute(OAuth2User principal, String... attributeNames) {
    if (principal == null) {
      return "";
    }
    return Stream.of(attributeNames)
        .map(it -> Optional.ofNullable((String) principal.getAttribute(it)).orElse(""))
        .filter(it -> !it.isBlank())
        .findFirst()
        .orElse("");
  }
}
