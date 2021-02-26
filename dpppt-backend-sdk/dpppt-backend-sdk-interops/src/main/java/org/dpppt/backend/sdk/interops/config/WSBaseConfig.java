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

import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.data.gaen.JDBCGAENDataServiceImpl;
import org.dpppt.backend.sdk.interops.syncer.IrishHubSyncer;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public abstract class WSBaseConfig implements SchedulingConfigurer, WebMvcConfigurer {

  @Value("${ws.retentiondays: 14}")
  int retentionDays;

  @Value("${ws.exposedlist.releaseBucketDuration: 7200000}")
  long releaseBucketDuration;

  @Value("${ws.app.gaen.timeskew:PT2h}")
  Duration timeSkew;

  @Value("${ws.origin.country}")
  String originCountry;

  @Value("${ws.international.countries:}")
  List<String> otherCountries;

  @Value("${ws.interops.irishbaseurl}")
  String irishBaseUrl;

  @Value("${ws.interops.irishauthorizationtoken}")
  String irishAuthorizationToken;

  @Value("${ws.interops.pemEncodedRSAPrivateKey}")
  String pemEncodedRSAPrivateKey;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  public abstract DataSource dataSource();

  public abstract Flyway flyway();

  public abstract String getDbType();

  @Bean
  public GAENDataService gaenDataService() {
    return new JDBCGAENDataServiceImpl(
        getDbType(),
        dataSource(),
        Duration.ofMillis(releaseBucketDuration),
        timeSkew,
        originCountry);
  }

  public IrishHubSyncer irishHubSyncer() {
    return new IrishHubSyncer(
        irishBaseUrl,
        irishAuthorizationToken,
        pemEncodedRSAPrivateKey,
        Duration.ofDays(retentionDays),
        Duration.ofMillis(releaseBucketDuration),
        gaenDataService(),
        originCountry);
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    taskRegistrar.addFixedRateTask(
        new IntervalTask(
            () -> {
              irishHubSyncer().sync();
            },
            Long.MAX_VALUE));
  }
}
