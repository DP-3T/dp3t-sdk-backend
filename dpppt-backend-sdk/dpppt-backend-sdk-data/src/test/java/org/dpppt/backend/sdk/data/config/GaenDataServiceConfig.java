/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.data.config;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import org.dpppt.backend.sdk.data.JDBCRedeemDataServiceImpl;
import org.dpppt.backend.sdk.data.RedeemDataService;
import org.dpppt.backend.sdk.data.gaen.FakeKeyService;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.data.gaen.JDBCGAENDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class GaenDataServiceConfig {

  @Value("${ws.gaen.randomkeysenabled: true}")
  boolean randomkeysenabled;

  @Value("${ws.exposedlist.releaseBucketDuration: 7200000}")
  long releaseBucketDuration;

  // @Value("${ws.app.gaen.timeskew:PT2h}")
  Duration timeSkew = Duration.ofHours(2);

  @Autowired DataSource dataSource;

  @Bean
  public DataSource fakeDataSource() {
    return new EmbeddedDatabaseBuilder()
        .generateUniqueName(true)
        .setType(EmbeddedDatabaseType.HSQL)
        .build();
  }

  @Autowired String dbType;

  @Bean
  public GAENDataService gaenDataService() {
    return new JDBCGAENDataServiceImpl(
        dbType,
        dataSource,
        Duration.ofMillis(releaseBucketDuration),
        timeSkew,
        "CH",
        List.of("DE", "IT"));
  }

  @Bean
  public PlatformTransactionManager transactionManger() {
    return new DataSourceTransactionManager(dataSource);
  }

  @Bean
  public RedeemDataService redeemDataService() {
    return new JDBCRedeemDataServiceImpl(dataSource);
  }

  @Bean
  public GAENDataService fakeService() {
    return new JDBCGAENDataServiceImpl(
        "hsql",
        fakeDataSource(),
        Duration.ofMillis(releaseBucketDuration),
        timeSkew,
        "CH",
        List.of("DE", "IT"));
  }

  @Bean
  public FakeKeyService fakeKeyService() throws NoSuchAlgorithmException {
    return new FakeKeyService(fakeService(), 10, 16, Duration.ofDays(21), randomkeysenabled);
  }
}
