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

import de.gematik.bbriccs.cardterminal.CardInfo;
import de.gematik.bbriccs.crypto.CryptoSystem;
import de.gematik.bbriccs.konnektor.Konnektor;
import de.gematik.bbriccs.konnektor.requests.GetCardHandleRequest;
import de.gematik.bbriccs.konnektor.requests.SignXMLDocumentRequest;
import de.gematik.bbriccs.rest.HttpBClient;
import de.gematik.bbriccs.rest.HttpBRequest;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.bbriccs.smartcards.SmcB;
import de.gematik.erpione.data.actor.ErpioneInstitutionData;
import de.gematik.erpione.data.meta.TIHealthCheckResult;
import de.gematik.erpione.data.meta.TIHealthCheckStatus;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.usecases.ErpBaseCommand;
import de.gematik.test.erezept.fhir.valuesets.IsoCountryCodeNCPeH;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hl7.fhir.r4.model.Resource;

@Getter
@Slf4j
@RequiredArgsConstructor
public class LEInstitution implements HealthcareInstitution {

  private final ErpioneInstitutionData info;
  private final ErpClient erpClient;
  private final HttpBClient erpProbingClient;
  private final Konnektor konnektor;
  private final SmcB smcB; // SMC-B belongs to the institution

  private final HermesService hermes;
  private final ErpionePersistence repository;

  private CardInfo smcbHandle;

  LEInstitution(InstitutionBuilder builder) {
    this.info = builder.info();
    this.erpClient = builder.erpClient();
    this.erpProbingClient = builder.erpProbingClient();
    this.konnektor = builder.konnektor();
    this.smcB = builder.smcB();

    this.hermes = builder.hermes();
    this.repository = builder.repository();
  }

  /** */
  @Override
  public void initialize() {
    this.erpClient.authenticateWith(smcB);
    this.erpClient.initialize();
    this.smcbHandle =
        this.konnektor.execute(GetCardHandleRequest.forSmartcard(this.smcB)).getPayload();
  }

  /**
   * @return
   */
  @Override
  public IsoCountryCodeNCPeH getCountryCode() {
    return IsoCountryCodeNCPeH.fromCode(this.info.getCountryCode());
  }

  /**
   * Telematik ID of the healthcare institution (LEI) from its <b>SMC-B</b>
   *
   * @return the TelematikId
   */
  @Override
  public String getTelematikId() {
    return this.smcB.getTelematikId();
  }

  /**
   * sign a plain string with the actor's signing smartcard
   *
   * @param document
   * @return
   */
  @Override
  public byte[] signDocument(String document) {
    log.trace("\n---- Signing document with SMC-B ----\n{}\n----", document);
    val request = new SignXMLDocumentRequest(this.smcbHandle, document, CryptoSystem.ECC_256);
    return konnektor.execute(request).getPayload();
  }

  /**
   * Refreshes the IDP token for this actor. This can be used to implement asynchronous token
   * refreshes
   */
  @Override
  public void refreshIdpToken() {
    this.erpClient.refreshIdpToken();
  }

  @Override
  public TIHealthCheckResult erpHealthCheck() {
    val request = HttpBRequest.get().withoutPayload();
    val start = System.currentTimeMillis();
    val response = this.erpProbingClient.send(request);
    val duration = System.currentTimeMillis() - start;

    val statusCode = response.statusCode();
    val status = TIHealthCheckStatus.fromHttpStatusCode(statusCode);
    return new TIHealthCheckResult(status, statusCode, duration);
  }

  /**
   * Perform a request against the E-Rezept Fachdienst
   *
   * @param request
   * @return
   */
  @Override
  public <R extends Resource> FhirBResponse<R> request(ErpBaseCommand<R> request) {
    return ErpRequestHelper.requestSafely(this, this.erpClient, request);
  }
}
