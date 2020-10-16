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
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2_0_3__SetOriginForExistingKeys extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    // HSQLDB: For Testing purposes only, set origin to CH
    String originCountry = "CH";

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
