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
import org.dpppt.backend.sdk.data.interops.JdbcSyncLogDataServiceImpl;
import org.dpppt.backend.sdk.data.interops.SyncLogDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SyncLogDataServiceConfig {

  @Autowired DataSource dataSource;

  @Autowired String dbType;

  @Bean
  public SyncLogDataService syncLogDataService() {
    return new JdbcSyncLogDataServiceImpl(dbType, dataSource);
  }
}
