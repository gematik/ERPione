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

package de.gematik.erpione.server.actor;

import de.gematik.erpione.data.actor.ErpioneInstitutionData;
import de.gematik.erpione.data.actor.InstitutionERole;
import de.gematik.erpione.server.actor.error.ActorNotFoundError;
import de.gematik.erpione.server.actor.impl.MnemonicUtils;
import java.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class ErpioneActorService {

  @Getter private final List<ErpioneInstitutionData> actorsInformation;

  private final boolean onlineMode;
  private final Map<String, HealthcareProvider> healthcareProviders;
  private final Map<String, HealthcareInstitution> healthcareInstitutions;

  private ErpioneActorService(Builder builder, boolean onlineMode) {
    this.onlineMode = onlineMode;

    this.actorsInformation = builder.actorsInformation;

    this.healthcareInstitutions = builder.healthcareInstitutions;
    this.healthcareProviders = builder.healthcareProviders;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Scheduled(fixedDelay = 4 * 60 * 1000) // every 4 minutes
  public void refreshIdpTokens() {
    if (onlineMode) {
      // this should help with the need of fetching a fresh IDP token on a concrete Request
      log.info("scheduled IDP token refresh");
      healthcareInstitutions.values().forEach(HealthcareInstitution::refreshIdpToken);
    }
  }

  /**
   * MID is a mnemonic id, which is a trimmed version of the mnemonic name
   *
   * @param id
   * @param actors
   * @param <A>
   * @return
   */
  private <A extends ErpioneActor> Optional<A> getActorByMID(String id, Map<String, A> actors) {
    // create a mnemonic id from the given id
    val mid = MnemonicUtils.mnemonicToId(id);
    return Optional.ofNullable(actors.get(id)).or(() -> Optional.ofNullable(actors.get(mid)));
  }

  public Optional<HealthcareProvider> getHealthcareProvider(String id) {
    return getActorByMID(id, healthcareProviders);
  }

  public HealthcareProvider getHealthcareProviderOrThrow(String id) {
    return getHealthcareProvider(id).orElseThrow(() -> new ActorNotFoundError(id));
  }

  public List<ErpioneInstitutionData> getInstitutionByRole(@Nullable InstitutionERole type) {
    return Optional.ofNullable(type)
        .map(it -> actorsInformation.stream().filter(ainf -> ainf.getRole().equals(it)).toList())
        .orElse(List.of());
  }

  public Optional<HealthcareInstitution> getInstitution(String id) {
    return getActorByMID(id, healthcareInstitutions);
  }

  public HealthcareInstitution getInstitutionOrThrow(String id) {
    return getInstitution(id).orElseThrow(() -> new ActorNotFoundError(id));
  }

  public static class Builder {
    private final List<ErpioneInstitutionData> actorsInformation = new ArrayList<>();
    private final Map<String, HealthcareProvider> healthcareProviders = new HashMap<>();
    private final Map<String, HealthcareInstitution> healthcareInstitutions = new HashMap<>();

    /**
     * A doctor is a healthcare provider always associated with a healthcare institution.
     *
     * @param doctors
     * @return
     */
    public Builder healthcareProvider(
        List<Pair<HealthcareInstitution, HealthcareProvider>> doctors) {
      val leis = doctors.stream().map(Pair::getFirst).toList();
      val les = doctors.stream().map(Pair::getSecond).toList();
      return this.withHealthcareInstitutions(leis).withHealthcareProviders(les);
    }

    private Builder withHealthcareProviders(List<HealthcareProvider> healthcareProviders) {
      healthcareProviders.forEach(hcp -> this.healthcareProviders.put(hcp.getId(), hcp));
      return this;
    }

    private Builder withHealthcareInstitutions(List<HealthcareInstitution> healthcareProviders) {
      healthcareProviders.forEach(
          hci -> {
            this.healthcareInstitutions.put(hci.getId(), hci);
            this.actorsInformation.add(hci.getInfo());
          });
      return this;
    }

    public Builder withKtrs(List<HealthcareInstitution> ktrs) {
      return this.withHealthcareInstitutions(ktrs);
    }

    public Builder withNcpEhs(List<HealthcareInstitution> ncpehs) {
      return this.withHealthcareInstitutions(ncpehs);
    }

    public ErpioneActorService build(boolean onlineMode) {
      if (onlineMode) {
        // connect all actors to the FD and IDP
        this.healthcareProviders.values().forEach(HealthcareProvider::initialize);
        this.healthcareInstitutions.values().forEach(HealthcareInstitution::initialize);
      }

      return new ErpioneActorService(this, onlineMode);
    }
  }
}
