/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package db.migration.pgsql;

import java.sql.PreparedStatement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V2_0_1__SetOriginForExistingKeys extends BaseJavaMigration {

  private static final String ORIGIN_COUNTRY_SYS_VAR = "ws.origin.country";

  private static final Logger logger =
      LoggerFactory.getLogger(V2_0_1__SetOriginForExistingKeys.class);

  @Override
  public void migrate(Context context) throws Exception {

    String originCountry = System.getProperty(ORIGIN_COUNTRY_SYS_VAR);
    if (originCountry == null || originCountry.isBlank()) {
      throw new IllegalArgumentException(
          "For successfull migration to the DP3T V2 database schema the country of origin must be"
              + " specified as system variable: "
              + ORIGIN_COUNTRY_SYS_VAR
              + " (for example: java -jar dp3t-sdk.jar -Dws.origin.country=CH ... )");
    }

    logger.info("Migrate all existing keys to origin country: " + originCountry);

    // Update all existing keys with no origin to the given origin country
    try (PreparedStatement update =
        context
            .getConnection()
            .prepareStatement("update t_gaen_exposed set origin = ? where origin is null")) {
      update.setString(1, originCountry);
      update.execute();
    }
  }
}
