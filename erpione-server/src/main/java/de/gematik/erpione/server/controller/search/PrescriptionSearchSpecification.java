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

package de.gematik.erpione.server.controller.search;

import static java.text.MessageFormat.format;

import de.gematik.erpione.server.jpa.entity.gem.ErpETaskEntity;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PrescriptionSearchSpecification {

  public static Specification<ErpETaskEntity> fromQueryParams(Map<String, String> params) {
    val acceptedParams = filterAcceptedParameters(params);

    return ((root, query, criteriaBuilder) -> {
      val predicates =
          acceptedParams.entrySet().stream()
              .map(
                  entry -> {
                    val value = entry.getValue();
                    val key = entry.getKey().getPath();
                    Path<Object> path = null;
                    if (key.contains(".")) {
                      // Handle nested properties
                      val parts = Arrays.stream(key.split("\\.")).toList();
                      for (val part : parts) {
                        path = Objects.requireNonNullElse(path, root).get(part);
                      }
                    } else {
                      // Handle direct properties
                      path = root.get(key);
                    }
                    return criteriaBuilder.equal(path, value);
                  })
              .toArray(Predicate[]::new);

      return criteriaBuilder.and(predicates);
    });
  }

  private static Map<PrescriptionSearchArgument, String> filterAcceptedParameters(
      Map<String, String> params) {
    val matchingArguments =
        params.entrySet().stream()
            .map(
                entry ->
                    PrescriptionSearchArgument.fromString(entry.getKey())
                        .map(param -> Map.entry(param, entry.getValue()))
                        .orElse(null))
            .filter(Objects::nonNull)
            .toList();

    // check for invalid values for better feedback on the API
    val unknownArguments =
        params.entrySet().stream()
            .filter(entry -> PrescriptionSearchArgument.fromString(entry.getKey()).isEmpty())
            .map(entry -> format("unknown Query: {0} = {1}", entry.getKey(), entry.getValue()));

    val invalidValues =
        matchingArguments.stream()
            .filter(entry -> !entry.getKey().matchesValue(entry.getValue()))
            .map(entry -> format("invalid Query: {0} = {1}", entry.getKey(), entry.getValue()));

    val issues = Stream.concat(unknownArguments, invalidValues).toList();

    if (!issues.isEmpty()) {
      throw new IllegalArgumentException("Invalid search parameters: " + String.join(", ", issues));
    }

    return matchingArguments.stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, entry -> entry.getKey().mapToValue(entry.getValue())));
  }
}
