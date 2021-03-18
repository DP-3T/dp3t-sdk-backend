/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.interops.insertmanager.insertionfilters;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.utils.UTCInstant;

/**
 * Checks if a key is in the configured retention period. If a key is before the retention period it
 * is filtered out, as it will not be relevant for the system anymore.
 */
public class EnforceRetentionPeriod implements InteropsKeyInsertionFilter {

  private final Duration retentionPeriod;

  public EnforceRetentionPeriod(Duration retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }

  @Override
  public List<GaenKeyForInterops> filter(UTCInstant now, List<GaenKeyForInterops> content) {
    return content.stream()
        .filter(
            key -> {
              var timestamp = UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
              return !timestamp.isBeforeDateOf(now.minus(retentionPeriod));
            })
        .collect(Collectors.toList());
  }

  @Override
  public String getName() {
    return "EnforeRetentionPeriod";
  }
}
