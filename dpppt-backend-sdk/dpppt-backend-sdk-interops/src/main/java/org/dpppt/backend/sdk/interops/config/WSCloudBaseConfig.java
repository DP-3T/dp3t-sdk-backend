/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.interops.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public abstract class WSCloudBaseConfig extends WSBaseConfig {

  @Autowired @Lazy private DataSource dataSource;

  @Override
  public DataSource dataSource() {
    return dataSource;
  }

  @Bean
  @Override
  public Flyway flyway() {
    Flyway flyWay =
        Flyway.configure()
            .dataSource(dataSource())
            .locations("classpath:/db/migration/pgsql_cluster")
            .load();
    flyWay.migrate();
    return flyWay;
  }

  @Override
  public String getDbType() {
    return "pgsql";
  }
}
