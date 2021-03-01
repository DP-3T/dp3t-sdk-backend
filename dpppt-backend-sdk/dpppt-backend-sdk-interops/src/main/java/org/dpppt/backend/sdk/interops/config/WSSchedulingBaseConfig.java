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

import org.dpppt.backend.sdk.interops.syncer.EfgsHubSyncer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class WSSchedulingBaseConfig {

  private final EfgsHubSyncer efgsHubSyncer;

  public WSSchedulingBaseConfig(EfgsHubSyncer efgsHubSyncer) {
    this.efgsHubSyncer = efgsHubSyncer;
  }

  @Scheduled(cron = "${ws.interops.efgs.syncCron: 0 0/10 * * * ?}")
  public void efgsHubSync() {
    efgsHubSyncer.sync();
  }
}
