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
import org.dpppt.backend.sdk.utils.UTCInstant;

/**
 * This filter checks for valid rolling period. The rolling period must always be in [1..144],
 * otherwise the key is not valid and is filtered out. See <a href=
 * "https://github.com/google/exposure-notifications-server/blob/main/docs/server_functional_requirements.md#publishing-temporary-exposure-keys"
 * >EN documentation</a>
 */
public class EnforceValidRollingPeriod implements InteropsKeyInsertionFilter {

  @Override
  public List<GaenKeyForInterops> filter(UTCInstant now, List<GaenKeyForInterops> content) {
    return content.stream()
        .filter(key -> key.getRollingPeriod() >= 1 && key.getRollingPeriod() <= 144)
        .collect(Collectors.toList());
  }

  @Override
  public String getName() {
    return "EnforeValidRollingPeriod";
  }
}
