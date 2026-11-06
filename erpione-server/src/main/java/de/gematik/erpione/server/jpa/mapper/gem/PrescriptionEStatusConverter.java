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

package de.gematik.erpione.server.jpa.mapper.gem;

import de.gematik.erpione.data.r4.etype.PrescriptionEStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts {@link PrescriptionEStatus} as code for database storage and vice versa. By storing the
 * code (instead of the entire enum), we save space in the database and can query easier by the code
 * (which is well known and used be the user).
 *
 * <p>This converter is auto-applied to all entities that use the {@link PrescriptionEStatus} type.
 */
@Converter(autoApply = true)
public class PrescriptionEStatusConverter
    implements AttributeConverter<PrescriptionEStatus, String> {
  /**
   * @param status
   * @return
   */
  @Override
  public String convertToDatabaseColumn(PrescriptionEStatus status) {
    return status.getCode();
  }

  /**
   * @param s
   * @return
   */
  @Override
  public PrescriptionEStatus convertToEntityAttribute(String s) {
    return PrescriptionEStatus.fromString(s);
  }
}
