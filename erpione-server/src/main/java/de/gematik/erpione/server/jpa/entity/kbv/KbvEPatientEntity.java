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

package de.gematik.erpione.server.jpa.entity.kbv;

import de.gematik.erpione.server.jpa.entity.HumanENameEmbeddable;
import jakarta.persistence.*;
import java.sql.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

@Data
@NoArgsConstructor
@Entity
@Table(name = "PATIENTS")
@EqualsAndHashCode(callSuper = true)
public class KbvEPatientEntity extends AbstractPersistable<Long> {
  private String kvnr;
  @Embedded private HumanENameEmbeddable name;
  private Date birthDate;
  private String city;
  private String postal;
  private String street;
}
