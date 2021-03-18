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

import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.utils.UTCInstant;

/**
 * Reject keys that are too far in the future. The `rollingStart` must not be later than tomorrow.
 */
public class RemoveKeysFromFuture implements InteropsKeyInsertionFilter {

  @Override
  public List<GaenKeyForInterops> filter(UTCInstant now, List<GaenKeyForInterops> content) {
    return content.stream()
        .filter(
            key -> {
              var rollingStartNumberInstant =
                  UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
              return rollingStartNumberInstant.isBeforeDateOf(now.plusDays(2));
            })
        .collect(Collectors.toList());
  }

  @Override
  public String getName() {
    return "RemoveKeysFromFuture";
  }
}
