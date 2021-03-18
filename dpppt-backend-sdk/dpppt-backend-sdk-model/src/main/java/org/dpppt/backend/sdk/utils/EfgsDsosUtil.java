/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.utils;

import java.time.LocalDateTime;
import java.time.Period;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;

public class EfgsDsosUtil {

  public static int calculateDefaultDsosMapping(GaenKeyForInterops gaenKey) {
    LocalDateTime rollingStartNumber =
        UTCInstant.of(gaenKey.getRollingStartNumber(), GaenUnit.TenMinutes).getLocalDateTime();
    LocalDateTime receivedAt = gaenKey.getReceivedAt().getLocalDateTime();

    int daysSinceSubmission =
        Period.between(receivedAt.toLocalDate(), rollingStartNumber.toLocalDate()).getDays();
    return daysSinceSubmission + 2000;
  }
}
