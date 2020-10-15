/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package db.migration.hsqldb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V2_0_1__SetVisitedForExistingKeys extends BaseJavaMigration {

  private static final Logger logger =
      LoggerFactory.getLogger(V2_0_1__SetVisitedForExistingKeys.class);

  @Override
  public void migrate(Context context) throws Exception {

    // HSQLDB: For Testing purposes only, set origin to CH
    String originCountry = "CH";

    // Get all key ids
    List<Integer> keyIds = new ArrayList<>();
    try (Statement select = context.getConnection().createStatement()) {
      try (ResultSet rows = select.executeQuery("select pk_exposed_id from t_gaen_exposed")) {
        while (rows.next()) {
          int id = rows.getInt(1);
          keyIds.add(id);
        }
      }
    }
    logger.info("Found " + keyIds.size() + " keys for migration");

    // For each key, insert origin country as visited country
    if (!keyIds.isEmpty()) {
      try (PreparedStatement insertVisitedCountries =
          context
              .getConnection()
              .prepareStatement("insert into t_visited(pfk_exposed_id, country) values(?, ?)")) {
        for (Integer keyId : keyIds) {
          insertVisitedCountries.setInt(1, keyId);
          insertVisitedCountries.setString(2, originCountry);
          insertVisitedCountries.addBatch();
        }
        insertVisitedCountries.executeBatch();
      }
    }
  }
}
