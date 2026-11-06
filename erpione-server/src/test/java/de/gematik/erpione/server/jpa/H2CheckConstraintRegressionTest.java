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

package de.gematik.erpione.server.jpa;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.sql.SQLException;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Regression test for <a
 * href="https://github.com/h2database/h2database/issues/4302">h2database#4302</a>.
 *
 * <p>Hibernate generates {@code check (col in (...))} constraints for the converted enum columns of
 * {@code ErpETaskEntity} (status, flowType). H2 2.4.240 failed to evaluate such constraints with
 * {@code 23514 Check constraint invalid} as soon as the connection that created the schema was
 * closed and the insert was executed on a different connection (which is the normal case with a
 * connection pool). This test must fail with H2 2.4.240 and pass with H2 >= 2.5.250.
 */
class H2CheckConstraintRegressionTest {

  private static final String URL = "jdbc:h2:mem:h2CheckRegression;DB_CLOSE_DELAY=-1";
  private static final int CHECK_CONSTRAINT_VIOLATED = 23513;

  @AfterEach
  void shutdown() throws SQLException {
    try (val c = DriverManager.getConnection(URL);
        val st = c.createStatement()) {
      st.execute("SHUTDOWN");
    }
  }

  @Test
  void shouldEvaluateInCheckConstraintFromAnotherConnection() throws SQLException {
    // connection 1: create the schema and close the connection afterwards
    try (val c1 = DriverManager.getConnection(URL);
        val st = c1.createStatement()) {
      st.execute(
          "create table task (id bigint primary key, status varchar(255) check ((status in"
              + " ('READY','CANCELLED','COMPLETED','INPROGRESS'))))");
    }

    // connection 2: the constraint must be evaluated correctly from a different session
    try (val c2 = DriverManager.getConnection(URL);
        val ps = c2.prepareStatement("insert into task (id, status) values (?, ?)")) {
      ps.setLong(1, 1L);
      ps.setString(2, "READY");
      assertEquals(1, assertDoesNotThrow(() -> ps.executeUpdate()));

      ps.setLong(1, 2L);
      ps.setString(2, "UNKNOWN");
      val ex = assertThrows(SQLException.class, ps::executeUpdate);
      assertEquals(CHECK_CONSTRAINT_VIOLATED, ex.getErrorCode());
    }

    try (val c3 = DriverManager.getConnection(URL);
        val st = c3.createStatement();
        val rs = st.executeQuery("select count(*) from task")) {
      assertTrue(rs.next());
      assertEquals(1, rs.getInt(1));
    }
  }
}
