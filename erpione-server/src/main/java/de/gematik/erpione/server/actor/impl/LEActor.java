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

import com.ibm.icu.impl.Pair;
import de.gematik.bbriccs.cardterminal.CardInfo;
import de.gematik.bbriccs.crypto.CryptoSystem;
import de.gematik.bbriccs.konnektor.Konnektor;
import de.gematik.bbriccs.konnektor.requests.GetCardHandleRequest;
import de.gematik.bbriccs.konnektor.requests.SignXMLDocumentRequest;
import de.gematik.bbriccs.konnektor.requests.options.SignDocumentOptions;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.bbriccs.smartcards.Hba;
import de.gematik.erpione.data.actor.ErpioneInstitutionData;
import de.gematik.erpione.data.r4.gem.ErpESignature;
import de.gematik.erpione.server.actor.HealthcareProvider;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.usecases.ErpBaseCommand;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bouncycastle.util.encoders.Base64;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
public class LEActor implements HealthcareProvider {

  private final ErpioneInstitutionData actorInfo;
  private final ErpClient erpClient;
  private final Konnektor konnektor;
  @Getter private final Hba hba; // HBA belongs to the concrete actor

  @Getter private final HermesService hermes;
  @Getter private final ErpionePersistence repository;

  private CardInfo hbaHandle;

  LEActor(HealthcareProviderBuilder builder) {
    this.actorInfo = builder.info();
    this.erpClient = builder.erpClient();
    this.konnektor = builder.konnektor();
    this.hba = builder.hba();

    this.hermes = builder.hermes();
    this.repository = builder.repository();
  }

  @Override
  public void initialize() {
    this.hbaHandle = konnektor.execute(GetCardHandleRequest.forSmartcard(this.hba)).getPayload();
  }

  @Override
  public ErpioneInstitutionData getInfo() {
    return actorInfo;
  }

  /**
   * The Telematik ID of the healthcare provider (LE) from his/her <b>HBA</b>
   *
   * @return
   */
  @Override
  public String getTelematikId() {
    return this.hba.getTelematikId();
  }

  @SneakyThrows
  @Override
  public Pair<byte[], ErpESignature> signDocument(String document) {
    log.trace("\n---- Signing document with HBA ----\n{}\n----", document);
    val cryptoSystem = CryptoSystem.DEFAULT_CRYPTO_SYSTEM;
    val tid = this.hba.getTelematikId();
    val signer = this.hba.getOwnerData().getOwnerName();

    val options = SignDocumentOptions.withAlgorithm(cryptoSystem);
    val documentContent = document.getBytes(StandardCharsets.UTF_8);
    val request = new SignXMLDocumentRequest(this.hbaHandle, documentContent, options, false);
    val signed = konnektor.execute(request).getPayload();
    val signedB64 = Base64.toBase64String(signed);

    val signedData = new Date();
    val signatureType = options.getSignatureType().getUrn();
    val mimeType = options.getMimeType();
    val cardType = this.hbaHandle.getType().value();
    val signature =
        new ErpESignature(signedData, tid, signer, cardType, signatureType, mimeType, signedB64);
    return Pair.of(signed, signature);
  }

  @Override
  public <R extends Resource> FhirBResponse<R> request(ErpBaseCommand<R> request) {
    return ErpRequestHelper.requestSafely(this, this.erpClient, request);
  }
}
