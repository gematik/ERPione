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

package de.gematik.erpione.server.hermes.impl;

import de.gematik.erpione.data.r4.HumanENameIn;
import de.gematik.erpione.data.r4.HumanENameOut;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class HermesUtils {

  private static boolean isNullOrBlank(String value) {
    return value == null || value.isBlank();
  }

  private static <T> boolean isNullOrEmpty(T value) {
    if (value == null) {
      return true;
    } else if (value instanceof String strValue) {
      return isNullOrBlank(strValue);
    } else {
      return false;
    }
  }

  @NonNull
  public static <T> T requireNonNullElseGet(T input, Supplier<T> defaultSupplier) {
    if (isNullOrEmpty(input)) {
      return defaultSupplier.get();
    } else {
      return input;
    }
  }

  public static Optional<String> ofNullable(String value) {
    return Optional.ofNullable(value).filter(it -> !isNullOrBlank(it));
  }

  /**
   * Completes a possibly partial or absent patient name into a fully usable one.
   *
   * <p>Any required name part ({@code given}, {@code family}) that is {@code null} in the input is
   * filled with a randomly generated value. An explicitly given {@code prefix} is passed through
   * unchanged; a missing one is filled occasionally via {@link GemFaker#generateNamePrefix()} and
   * may therefore remain {@code null}.
   *
   * @param nameIn the name as provided by the caller; may be {@code null} or contain {@code null}
   *     fields
   * @return a non-null name whose {@code given} and {@code family} parts are guaranteed to be
   *     non-null
   */
  public static HumanENameOut completeName(@Nullable HumanENameIn nameIn) {
    return completeName(nameIn, 0.0);
  }

  public static HumanENameOut completeName(@Nullable HumanENameIn nameIn, double prefixChance) {
    nameIn = HermesUtils.requireNonNullElseGet(nameIn, HumanENameIn::new);

    val prefix =
        ofNullable(nameIn.getPrefix())
            .orElseGet(
                () -> GemFaker.chanceOf(prefixChance, GemFaker::generateNamePrefix).orElse(null));
    val given = HermesUtils.requireNonNullElseGet(nameIn.getGiven(), GemFaker::fakerFirstName);
    val family = HermesUtils.requireNonNullElseGet(nameIn.getFamily(), GemFaker::fakerLastName);
    return new HumanENameOut(prefix, given, family);
  }
}
