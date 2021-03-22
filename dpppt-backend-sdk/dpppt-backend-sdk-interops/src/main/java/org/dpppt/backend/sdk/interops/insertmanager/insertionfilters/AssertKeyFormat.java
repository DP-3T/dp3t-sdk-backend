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

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;

/** Filters out keys with invalid base64 encoding or incorrect length. */
public class AssertKeyFormat implements InteropsKeyInsertionFilter {

  private final int gaenKeySizeBytes;

  public AssertKeyFormat(int gaenKeySizeBytes) {
    this.gaenKeySizeBytes = gaenKeySizeBytes;
  }

  @Override
  public List<GaenKeyForInterops> filter(UTCInstant now, List<GaenKeyForInterops> content) {
    return content.stream()
        .filter(key -> isValidKeyFormat(key.getKeyData()))
        .collect(Collectors.toList());
  }

  private boolean isValidKeyFormat(String value) {
    try {
      byte[] key = Base64.getDecoder().decode(value);
      return key.length == gaenKeySizeBytes;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String getName() {
    return "AssertKeyFormat";
  }
}
