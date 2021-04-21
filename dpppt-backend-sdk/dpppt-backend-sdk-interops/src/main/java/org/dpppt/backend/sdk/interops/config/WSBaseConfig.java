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

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import javax.sql.DataSource;
import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.data.gaen.JdbcGaenDataServiceImpl;
import org.dpppt.backend.sdk.data.interops.JdbcSyncLogDataServiceImpl;
import org.dpppt.backend.sdk.data.interops.SyncLogDataService;
import org.dpppt.backend.sdk.interops.insertmanager.InteropsInsertManager;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.AssertKeyFormat;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.EfgsDsosFilter;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.EnforceRetentionPeriod;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.EnforceValidRollingPeriod;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.RemoveKeysFromFuture;
import org.dpppt.backend.sdk.interops.model.HubConfigs;
import org.dpppt.backend.sdk.interops.syncer.EfgsHubSyncer;
import org.dpppt.backend.sdk.interops.syncer.efgs.EfgsClient;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public abstract class WSBaseConfig implements WebMvcConfigurer {

  @Value("${interops.retentiondays: 14}")
  int retentionDays;

  @Value("${interops.releaseBucketDuration: 7200000}")
  long releaseBucketDuration;

  @Value("${interops.gaen.key_size: 16}")
  int gaenKeySizeBytes;

  @Value("${interops.gaen.timeskew:PT2h}")
  Duration timeSkew;

  @Value("${interops.origin.country}")
  String originCountry;

  @Value("${interops.efgs.dsosfilter.enabled: true}")
  boolean efgsDsosFilterEnabled;

  @Value("${interops.efgs.dsosfilter.symptomaticOnsetKnown.dropDaysBeforeOnset: -2}")
  int efgsDsosFilterSymptomaticOnsetKnownDropDaysBeforeOnset;

  @Value("${interops.efgs.dsosfilter.symptomaticOnsetRange.dropDaysBeforeRangeStart: -2}")
  int efgsDsosFilterSymptomaticOnsetRangeDropDaysBeforeRangeStart;

  @Value("${interops.efgs.dsosfilter.symptomaticUnknownOnset.dropDaysBeforeSubmission: -2}")
  int efgsDsosFilterSymptomaticUnknownOnsetDropDaysBeforeSubmission;

  @Value("${interops.efgs.dsosfilter.asympomatic.dropDaysBeforeSubmission: -2}")
  int efgsDsosFilterAsymptomaticDropDaysBeforeSubmission;

  @Value("${interops.efgs.dsosfilter.unknownSymptomStatus.dropDaysBeforeSubmission: -2}")
  int efgsDsosFilterUnknownSymptomStatusDropDaysBeforeSubmission;

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
  public EfgsClient efgsClient(HubConfigs hubConfigs) throws CertificateException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, IOException {
    return new EfgsClient(hubConfigs.getEfgsGateways().get(0));
  }

  @Bean
  public EfgsHubSyncer efgsHubSyncer(
      EfgsClient efgsClient,
      GaenDataService gaenDataService,
      SyncLogDataService syncLogDataService,
      InteropsInsertManager interopsInsertManager) {
    return new EfgsHubSyncer(
        efgsClient,
        Duration.ofDays(retentionDays),
        gaenDataService,
        syncLogDataService,
        interopsInsertManager);
  }

  @Bean
  public InteropsInsertManager interopsInsertManager(GaenDataService gaenDataService) {
    var manager = new InteropsInsertManager(gaenDataService);
    manager.addFilter(new AssertKeyFormat(gaenKeySizeBytes));
    manager.addFilter(new RemoveKeysFromFuture());
    manager.addFilter(new EnforceRetentionPeriod(Duration.ofDays(retentionDays)));
    manager.addFilter(new EnforceValidRollingPeriod());
    if (efgsDsosFilterEnabled) {
      manager.addFilter(
          new EfgsDsosFilter(
              efgsDsosFilterSymptomaticOnsetKnownDropDaysBeforeOnset,
              efgsDsosFilterSymptomaticOnsetRangeDropDaysBeforeRangeStart,
              efgsDsosFilterSymptomaticUnknownOnsetDropDaysBeforeSubmission,
              efgsDsosFilterAsymptomaticDropDaysBeforeSubmission,
              efgsDsosFilterUnknownSymptomStatusDropDaysBeforeSubmission));
    }
    return manager;
  }
}
