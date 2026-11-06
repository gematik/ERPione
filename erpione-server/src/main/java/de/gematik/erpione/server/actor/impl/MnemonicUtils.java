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

package de.gematik.erpione.server.actor.impl;

import com.google.common.base.Strings;
import de.gematik.bbriccs.crypto.CryptoSystem;
import de.gematik.bbriccs.fhir.de.value.TelematikID;
import de.gematik.bbriccs.smartcards.Hba;
import de.gematik.bbriccs.smartcards.InstituteSmartcard;
import de.gematik.bbriccs.smartcards.Smartcard;
import de.gematik.erpione.data.actor.SmartcardEData;
import de.gematik.test.erezept.fhir.builder.GemFaker;
import java.util.Optional;
import lombok.Generated;
import lombok.SneakyThrows;
import lombok.val;
import org.bouncycastle.util.encoders.Base64;

public class MnemonicUtils {

  private static final int MAX_ID = 1000;

  @Generated
  private MnemonicUtils() {
    throw new IllegalAccessError("Utility class");
  }

  public static String mnemonicToId(String mnemonic) {
    val tm = trimForComparison(mnemonic);
    // positive hashCode() is used to avoid negative values
    val phash = (tm.hashCode() & 0xfffffff);
    // ensure the id is within the range of 0 to MAX_ID - 1
    val id = phash % MAX_ID;
    return String.valueOf(id);
  }

  /**
   * Extracts the common name from a Smartcard, removing "TEST-ONLY" if present. If the common name
   * is not available, it returns a random ancient god name.
   *
   * @param smartcard the Smartcard from which to extract the common name
   * @return the common name or a random ancient god name
   */
  public static String extractCommonNameFrom(Smartcard smartcard) {
    return Optional.ofNullable(smartcard)
        .flatMap(it -> Optional.ofNullable(it.getOwnerData()))
        .flatMap(it -> Optional.ofNullable(it.getCommonName()))
        .orElseGet(() -> GemFaker.getFaker().ancient().god());
  }

  public static String extractTelematikIdFrom(InstituteSmartcard smartcard) {
    return Optional.ofNullable(smartcard)
        .flatMap(it -> Optional.ofNullable(it.getTelematikId()))
        .orElseGet(() -> TelematikID.random().getValue());
  }

  @SneakyThrows
  public static SmartcardEData extractInfo(InstituteSmartcard smartcard) {
    val info = new SmartcardEData();
    info.setIccsn(smartcard.getIccsn());
    info.setTelematikId(extractTelematikIdFrom(smartcard));
    info.setPublicKey(encodeAuthPublicKey(smartcard));
    info.setValidUntil(smartcard.getAutCertificate().getX509Certificate().getNotAfter());

    val profession = smartcard.getProfession();
    info.setProfession(profession.getDisplay());
    info.setProfessionOid(profession.getValue());
    return info;
  }

  @SneakyThrows
  public static SmartcardEData extractInfo(Hba hba) {
    val info = extractInfo((InstituteSmartcard) hba);
    val certificate = hba.getQesCertificate(CryptoSystem.DEFAULT_CRYPTO_SYSTEM);
    val encoded = Base64.toBase64String(certificate.getX509Certificate().getEncoded());
    info.setCertificate(encoded);
    return info;
  }

  private static String encodeAuthPublicKey(Smartcard smartcard) {
    return Optional.ofNullable(smartcard)
        .flatMap(it -> Optional.ofNullable(it.getAuthPublicKey()))
        .map(it -> Base64.toBase64String(it.getEncoded()))
        .orElse("");
  }

  public static String trimForComparison(String str) {
    return Strings.nullToEmpty(str).trim().toLowerCase();
  }
}
