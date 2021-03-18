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
import javax.sql.DataSource;
import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.data.gaen.JdbcGaenDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GaenDataServiceConfig {

  @Value("${ws.exposedlist.releaseBucketDuration: 7200000}")
  long releaseBucketDuration;

  final String originCountry = "CH";

  final Duration timeSkew = Duration.ofHours(2);

  @Autowired DataSource dataSource;

  @Autowired String dbType;

  @Bean
  public GaenDataService gaenDataService() {
    return new JdbcGaenDataServiceImpl(
        dbType, dataSource, Duration.ofMillis(releaseBucketDuration), timeSkew, originCountry);
  }
}
