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
import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.interops.insertmanager.InteropsInsertManager;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.AssertKeyFormat;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.DsosFilter;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.EnforceRetentionPeriod;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.EnforceValidRollingPeriod;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.RemoveKeysFromFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InteropsInsertManagerConfig {

  @Value("${ws.retentiondays: 14}")
  int retentionDays;

  @Value("${ws.app.gaen.key_size: 16}")
  int gaenKeySizeBytes;

  @Bean
  public InteropsInsertManager interopsInsertManager(GaenDataService gaenDataService) {
    var manager = new InteropsInsertManager(gaenDataService);
    manager.addFilter(new AssertKeyFormat(gaenKeySizeBytes));
    manager.addFilter(new RemoveKeysFromFuture());
    manager.addFilter(new EnforceRetentionPeriod(Duration.ofDays(retentionDays)));
    manager.addFilter(new EnforceValidRollingPeriod());
    manager.addFilter(new DsosFilter());
    return manager;
  }
}
