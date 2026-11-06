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

package de.gematik.erpione.server.controller;

import static de.gematik.test.erezept.fhir.parser.ProfileFhirParserFactory.ERP_FHIR_PROFILES_CONFIG;
import static de.gematik.test.erezept.fhir.parser.ProfileFhirParserFactory.ERP_FHIR_PROFILES_TOGGLE;

import de.gematik.bbriccs.fhir.conf.ProfileDto;
import de.gematik.bbriccs.fhir.conf.ProfilesConfigurator;
import de.gematik.erpione.data.meta.*;
import de.gematik.erpione.server.actor.factory.ErpEnvironmentConfig;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = OpenApiETags.META_INFO_TAG)
@RequestMapping("/api/info")
@RequiredArgsConstructor
public class MetaController {

  private final ErpEnvironmentConfig environment;

  @Bean
  @Primary
  public OpenAPI openAPI() {
    val license =
        new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0");
    val contact = new Contact().name("Gematik").url("https://www.gematik.de/");

    val version =
        Optional.ofNullable(getClass().getPackage().getImplementationVersion())
            .orElse("DEV-SNAPSHOT");
    val apiKeyComponent =
        new Components()
            .addSecuritySchemes(
                "API-Key",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("apikey"));

    val info =
        new Info()
            .title("Erpione")
            .version(version)
            .description(
                "Erpione is a lightweight client simulator for the ePrescription service, built"
                    + " mainly for testing and experimentation.")
            .license(license)
            .contact(contact);

    val tags = OpenApiETags.getAllTags();

    return new OpenAPI().components(apiKeyComponent).tags(tags).info(info);
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ErpioneServiceInfo getErpioneInfo() {

    val buildInfo = ErpioneBuildInfo.Companion.fromEnvironment();

    val tiInfo = new TIConnectionData();
    tiInfo.addService(
        "idp", new TIServiceData(environment.getTi().getIdp().getDiscoveryDocumentUrl()));
    tiInfo.addService(
        "erp",
        new TIServiceData(environment.getTi().getFdBaseUrl(), environment.getVauCertificate()));

    val fhirInfo = createFhirInfo();

    return new ErpioneServiceInfo(buildInfo, fhirInfo, tiInfo);
  }

  private static ErpioneFhirInfo createFhirInfo() {
    val conf =
        ProfilesConfigurator.getConfiguration(ERP_FHIR_PROFILES_CONFIG, ERP_FHIR_PROFILES_TOGGLE);
    val defaultConf = conf.getDefaultProfile();
    val profiles =
        defaultConf.getProfiles().stream()
            .collect(
                Collectors.toMap(
                    ProfileDto::getName,
                    ProfileDto::getVersion,
                    // if a profile is configured multiple times (with different version), we show
                    // the first one
                    // which is the same behavior the FHIR-Builders will do
                    (v1, v2) -> v1));
    return new ErpioneFhirInfo(defaultConf.getId(), profiles);
  }
}
