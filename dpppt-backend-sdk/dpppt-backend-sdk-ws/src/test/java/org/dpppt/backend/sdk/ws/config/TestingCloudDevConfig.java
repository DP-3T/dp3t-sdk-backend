/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.backend.sdk.ws.config;

import java.util.Base64;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@Configuration
@Profile("test-cloud")
public class TestingCloudDevConfig extends WSCloudBaseConfig {

  @Value("${vcap.services.ecdsa_dev.credentials.privateKey}")
  private String privateKey;

  @Value("${vcap.services.ecdsa_dev.credentials.publicKey}")
  public String publicKey;

  @Override
  String getPrivateKey() {
    return new String(Base64.getDecoder().decode(privateKey));
  }

  @Override
  String getPublicKey() {
    return new String(Base64.getDecoder().decode(publicKey));
  }

  @Bean
  DataSource hsqlSource() {
    return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build();
  }

  @Bean
  @Override
  public Flyway flyway() {
    Flyway flyWay =
        Flyway.configure()
            .dataSource(dataSource())
            .locations("classpath:/db/migration/hsqldb")
            .load();
    flyWay.migrate();
    return flyWay;
  }

  @Override
  public String getDbType() {
    return "hsqldb";
  }
}
