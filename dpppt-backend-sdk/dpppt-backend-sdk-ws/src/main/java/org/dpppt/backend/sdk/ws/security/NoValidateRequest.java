/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.security;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.utils.UTCInstant;

public class NoValidateRequest implements ValidateRequest {

  @Override
  public boolean isValid(Object authObject) throws WrongScopeException {
    return true;
  }

  @Override
  public long validateKeyDate(UTCInstant now, Object authObject, Object others)
      throws ClaimIsBeforeOnsetException {
    if (others instanceof GaenKey) {
      GaenKey request = (GaenKey) others;
      var keyDate = UTCInstant.of(request.getRollingStartNumber(), GaenUnit.TenMinutes);
      return keyDate.getTimestamp();
    }
    throw new IllegalArgumentException();
  }

  @Override
  public boolean isFakeRequest(Object authObject, Object others) {
    if (others instanceof GaenKey) {
      GaenKey request = (GaenKey) others;
      boolean fake = false;
      if (request.getFake() == 1) {
        fake = true;
      }
      return fake;
    }
    throw new IllegalArgumentException();
  }
}
