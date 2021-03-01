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

import java.security.cert.CertificateException;
import java.time.Duration;
import javax.sql.DataSource;
import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.data.gaen.JdbcGaenDataServiceImpl;
import org.dpppt.backend.sdk.data.interops.JdbcSyncLogDataServiceImpl;
import org.dpppt.backend.sdk.data.interops.SyncLogDataService;
import org.dpppt.backend.sdk.interops.model.HubConfigs;
import org.dpppt.backend.sdk.interops.syncer.EfgsHubSyncer;
import org.dpppt.backend.sdk.interops.syncer.efgs.EfgsClient;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public abstract class WSBaseConfig implements WebMvcConfigurer {

  @Value("${ws.retentiondays: 14}")
  int retentionDays;

  @Value("${ws.exposedlist.releaseBucketDuration: 7200000}")
  long releaseBucketDuration;

  @Value("${ws.app.gaen.timeskew:PT2h}")
  Duration timeSkew;

  @Value("${ws.origin.country}")
  String originCountry;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  public abstract DataSource dataSource();

  public abstract Flyway flyway();

  public abstract String getDbType();

  @Bean
  public GaenDataService gaenDataService(DataSource dataSource) {
    return new JdbcGaenDataServiceImpl(
        getDbType(), dataSource, Duration.ofMillis(releaseBucketDuration), timeSkew, originCountry);
  }

  @Bean
  public SyncLogDataService syncLogDataService(DataSource dataSource) {
    return new JdbcSyncLogDataServiceImpl(getDbType(), dataSource);
  }

  @Bean
  public EfgsClient efgsClient(HubConfigs hubConfigs) throws CertificateException {
    return new EfgsClient(hubConfigs.getEfgsGateways().get(0));
  }

  @Bean
  public EfgsHubSyncer efgsHubSyncer(
      EfgsClient efgsClient,
      GaenDataService gaenDataService,
      SyncLogDataService syncLogDataService) {
    return new EfgsHubSyncer(
        efgsClient, Duration.ofDays(retentionDays), gaenDataService, syncLogDataService);
  }
}
