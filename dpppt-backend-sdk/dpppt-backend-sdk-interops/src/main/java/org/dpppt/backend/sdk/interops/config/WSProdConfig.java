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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class WSProdConfig extends WSBaseConfig {

  @Value("${datasource.username}")
  String dataSourceUser;

  @Value("${datasource.password}")
  String dataSourcePassword;

  @Value("${datasource.url}")
  String dataSourceUrl;

  @Value("${datasource.driverClassName}")
  String dataSourceDriver;

  @Value("${datasource.failFast}")
  String dataSourceFailFast;

  @Value("${datasource.maximumPoolSize}")
  String dataSourceMaximumPoolSize;

  @Value("${datasource.maxLifetime}")
  String dataSourceMaxLifetime;

  @Value("${datasource.idleTimeout}")
  String dataSourceIdleTimeout;

  @Value("${datasource.connectionTimeout}")
  String dataSourceConnectionTimeout;

  @Bean(destroyMethod = "close")
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    Properties props = new Properties();
    props.put("url", dataSourceUrl);
    props.put("user", dataSourceUser);
    props.put("password", dataSourcePassword);
    config.setDataSourceProperties(props);
    config.setDataSourceClassName(dataSourceDriver);
    config.setMaximumPoolSize(Integer.parseInt(dataSourceMaximumPoolSize));
    config.setMaxLifetime(Integer.parseInt(dataSourceMaxLifetime));
    config.setIdleTimeout(Integer.parseInt(dataSourceIdleTimeout));
    config.setConnectionTimeout(Integer.parseInt(dataSourceConnectionTimeout));
    return new HikariDataSource(config);
  }

  @Bean
  @Override
  public Flyway flyway() {
    Flyway flyWay =
        Flyway.configure()
            .dataSource(dataSource())
            .locations("classpath:/db/migration/pgsql")
            .load();
    flyWay.migrate();
    return flyWay;
  }

  @Override
  public String getDbType() {
    return "pgsql";
  }
}
