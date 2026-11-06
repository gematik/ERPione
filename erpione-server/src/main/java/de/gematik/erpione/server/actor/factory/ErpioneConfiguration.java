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

import de.gematik.bbriccs.rest.BasicHttpClient;
import de.gematik.bbriccs.smartcards.SmartcardArchive;
import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.server.actor.ErpioneActorService;
import de.gematik.erpione.server.actor.HealthcareInstitution;
import de.gematik.erpione.server.actor.HealthcareProvider;
import de.gematik.erpione.server.actor.impl.ErpioneActorBuilder;
import de.gematik.erpione.server.hermes.HermesService;
import de.gematik.erpione.server.hermes.impl.HermesServiceImpl;
import de.gematik.erpione.server.jpa.ErpionePersistence;
import de.gematik.test.erezept.fhir.parser.FhirParser;
import de.gematik.test.erezept.fhir.parser.ValidatorType;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.util.Pair;
import tools.jackson.databind.ObjectMapper;

@Data
@Configuration
@ConfigurationProperties(prefix = "erpione")
public class ErpioneConfiguration {

  private final ErpionePersistence repository;
  private final ErpEnvironmentConfig environment;
  private final ObjectMapper objectMapper;

  private boolean onlineMode = true;
  private String fhirProfile; // Note: does not work properly yet; set via System property instead

  private List<ErpioneActorConfiguration> pharmacists = List.of();
  private List<ErpioneActorConfiguration> doctors = List.of();
  private List<ErpioneActorConfiguration> ktrs = List.of();
  private List<ErpioneActorConfiguration> ncpehs = List.of();

  private FhirParser fhirCodec;
  private HermesService hermes;

  @Autowired
  public ErpioneConfiguration(
      ErpionePersistence repository, ErpEnvironmentConfig environment, ObjectMapper objectMapper) {
    this.repository = repository;
    this.environment = environment;
    this.objectMapper = objectMapper;
  }

  private void setEnvironment(ErpioneActorBuilder builder) {
    val vauCertificate = environment.getVauCertificate(onlineMode);
    val tiRoute = environment.getTi();

    val erpProbingClient =
        BasicHttpClient.forUrl(tiRoute.getFdBaseUrl())
            .userAgent(tiRoute.getUserAgent())
            .withoutTlsVerification();

    val fhirCode = createFhirParser();
    builder
        .erpClient(tiRoute.createErpClient(vauCertificate, fhirCode))
        .erpProbingClient(erpProbingClient)
        .hermes(hermes)
        .repository(repository);
  }

  private void setSmartcards(
      ErpioneActorBuilder builder, ErpioneActorConfiguration cfg, SmartcardArchive sca) {
    Optional.ofNullable(cfg.getSmcbIccsn())
        .map(sca::getSmcbByICCSN)
        .ifPresent(smcb -> builder.smcB(smcb, cfg.getCountryCode()));
    Optional.ofNullable(cfg.getHbaIccsn()).map(sca::getHbaByICCSN).ifPresent(builder::hba);
    Optional.ofNullable(cfg.getCustomName())
        .filter(it -> !it.isEmpty())
        .ifPresent(builder::customName);
  }

  private HealthcareInstitution createNcpeh(ErpioneActorConfiguration cfg, SmartcardArchive sca) {
    val builder = ErpioneActorBuilder.builderFor(cfg.getMnemonic(), InstitutionERole.NCP_EH);

    setSmartcards(builder, cfg, sca);
    setEnvironment(builder);

    return builder.konnektor(cfg.createKonnektor(sca)).buildInstitution();
  }

  private HealthcareInstitution createKtr(ErpioneActorConfiguration cfg, SmartcardArchive sca) {
    val smcb = sca.getSmcbByICCSN(cfg.getSmcbIccsn());

    val builder = ErpioneActorBuilder.healthInsurance(cfg.getMnemonic());

    setEnvironment(builder);
    return builder.smcB(smcb).konnektor(cfg.createKonnektor(sca)).buildInstitution();
  }

  private Pair<HealthcareInstitution, HealthcareProvider> createDoctor(
      ErpioneActorConfiguration cfg, SmartcardArchive sca) {
    val hba = sca.getHbaByICCSN(cfg.getHbaIccsn());
    val smcb = sca.getSmcbByICCSN(cfg.getSmcbIccsn());

    val builder = ErpioneActorBuilder.doctor(cfg.getMnemonic());

    setEnvironment(builder);
    return builder.hba(hba).smcB(smcb).konnektor(cfg.createKonnektor(sca)).build();
  }

  private Pair<HealthcareInstitution, HealthcareProvider> createPharmacist(
      ErpioneActorConfiguration cfg, SmartcardArchive sca) {
    val hba = sca.getHbaByICCSN(cfg.getHbaIccsn());
    val smcb = sca.getSmcbByICCSN(cfg.getSmcbIccsn());

    val builder = ErpioneActorBuilder.pharmacist(cfg.getMnemonic());

    setEnvironment(builder);
    return builder.hba(hba).smcB(smcb).konnektor(cfg.createKonnektor(sca)).build();
  }

  @Bean
  @Primary
  public ErpioneActorService createErpioneActorService() {
    val sca = SmartcardArchive.fromResources();

    val docActors = doctors.stream().map(it -> createDoctor(it, sca)).toList();
    val pharmActors = pharmacists.stream().map(it -> createPharmacist(it, sca)).toList();
    val ktrActors = ktrs.stream().map(it -> createKtr(it, sca)).toList();
    val ncpehActors = ncpehs.stream().map(it -> createNcpeh(it, sca)).toList();

    return ErpioneActorService.builder()
        .healthcareProvider(docActors)
        .healthcareProvider(pharmActors)
        .withKtrs(ktrActors)
        .withNcpEhs(ncpehActors)
        .build(onlineMode);
  }

  @Bean
  public FhirParser createFhirParser() {
    // this is a hack to ensure all fhir parsers are the same instance
    // observe and decide later if this is a good idea
    if (this.fhirCodec == null) {
      this.fhirCodec = new FhirParser(ValidatorType.NONE);
      this.hermes = new HermesServiceImpl(this.fhirCodec, this.objectMapper);
    }
    return fhirCodec;
  }
}
