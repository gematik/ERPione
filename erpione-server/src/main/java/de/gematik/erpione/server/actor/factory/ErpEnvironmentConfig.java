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

package de.gematik.erpione.server.actor.factory;

import static java.text.MessageFormat.format;

import com.google.common.base.Strings;
import de.gematik.bbriccs.rest.headers.AuthHttpHeaderKey;
import de.gematik.bbriccs.rest.headers.StandardHttpHeaderKey;
import de.gematik.test.erezept.client.UnirestRetryWrapper;
import de.gematik.test.erezept.client.vau.VauException;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Optional;
import kong.unirest.core.Unirest;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jose4j.base64url.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "erpione.environment")
public class ErpEnvironmentConfig {

  private static X509Certificate vauCertificateX509;

  private String vauCertificate;
  private ErpConnectionConfig.TiConnectionConfig ti;
  private ErpConnectionConfig.InternetConnectionConfig internet;

  public X509Certificate getVauCertificate(boolean onlineMode) {
    if (onlineMode) return requestCachedVauCertificate();
    else return getOfflineVauCertificate();
  }

  @SneakyThrows
  private X509Certificate getOfflineVauCertificate() {
    val certificateFactory = CertificateFactory.getInstance("X.509");
    val data = Base64.decode(vauCertificate);
    val bais = new ByteArrayInputStream(data);
    return (X509Certificate) certificateFactory.generateCertificate(bais);
  }

  private X509Certificate requestCachedVauCertificate() {
    if (vauCertificateX509 == null) {
      vauCertificateX509 = requestVauCertificate();
    }
    return vauCertificateX509;
  }

  @SneakyThrows
  private X509Certificate requestVauCertificate() {
    val certificateFactory = CertificateFactory.getInstance("X.509");
    val certUrl = format("{0}/VAUCertificate", ti.getFdBaseUrl());

    val headers = new HashMap<String, String>();
    Optional.ofNullable(ti.getXapiKey())
        .filter(it -> !Strings.isNullOrEmpty(it))
        .ifPresent(it -> AuthHttpHeaderKey.X_API_KEY.createHeader(it).apply(headers::put));

    Optional.ofNullable(ti.getUserAgent())
        .filter(it -> !Strings.isNullOrEmpty(it))
        .ifPresent(it -> StandardHttpHeaderKey.USER_AGENT.createHeader(it).apply(headers::put));

    log.info("Request {} Certificate from {}", certificateFactory.getType(), certUrl);
    val req = Unirest.get(certUrl).headers(headers);

    val retryWrapper = new UnirestRetryWrapper();

    try {
      val response = retryWrapper.requestWithRetries(req);
      val data = response.getBody();
      log.info(
          "Received response with {} bytes ({}) from VAU-Certificate with status code {}",
          data.length,
          response.getHeaders().getFirst("content-type"),
          response.getStatus());
      vauCertificate = Base64.encode(data);
      val byteArrayInputStream = new ByteArrayInputStream(data);
      val cert = (X509Certificate) certificateFactory.generateCertificate(byteArrayInputStream);
      log.info("Received VAU-Certificate successfully");
      return cert;
    } catch (CertificateException e) {
      throw new VauException("Error during Requesting VAU-Certificate", e);
    }
  }
}
