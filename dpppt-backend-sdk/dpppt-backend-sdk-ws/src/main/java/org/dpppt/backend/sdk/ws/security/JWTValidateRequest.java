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

import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.ExposeeRequestList;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.springframework.security.oauth2.jwt.Jwt;

public class JWTValidateRequest implements ValidateRequest {
  private final ValidationUtils validationUtils;

  public JWTValidateRequest(ValidationUtils validationUtils) {
    this.validationUtils = validationUtils;
  }

  @Override
  public boolean isValid(Object authObject) {
    if (authObject instanceof Jwt) {
      Jwt token = (Jwt) authObject;
      return token.containsClaim("scope") && token.getClaim("scope").equals("exposed");
    }
    return false;
  }

  @Override
  public long getKeyDate(UTCInstant now, Object authObject, Object others)
      throws InvalidDateException {
    if (authObject instanceof Jwt) {
      Jwt token = (Jwt) authObject;
      var jwtKeyDate = UTCInstant.parseDate(token.getClaim("onset"));
      if (others instanceof ExposeeRequest) {
        ExposeeRequest request = (ExposeeRequest) others;
        var requestKeyDate = UTCInstant.ofEpochMillis(request.getKeyDate());
        if (!validationUtils.isDateInRange(requestKeyDate, now)
            || requestKeyDate.isBeforeEpochMillisOf(jwtKeyDate)) {
          throw new InvalidDateException();
        }
        jwtKeyDate = UTCInstant.ofEpochMillis(request.getKeyDate());
      }
      return jwtKeyDate.getTimestamp();
    }
    throw new IllegalArgumentException();
  }

  @Override
  public boolean isFakeRequest(Object authObject, Object others) {
    if (authObject instanceof Jwt && others instanceof ExposeeRequest) {
      Jwt token = (Jwt) authObject;
      ExposeeRequest request = (ExposeeRequest) others;
      boolean fake = false;
      if (token.containsClaim("fake") && token.getClaimAsString("fake").equals("1")) {
        fake = true;
      }
      if (request.isFake() == 1) {
        fake = true;
      }
      return fake;
    }
    if (authObject instanceof Jwt && others instanceof ExposeeRequestList) {
      Jwt token = (Jwt) authObject;
      ExposeeRequestList request = (ExposeeRequestList) others;
      boolean fake = false;
      if (token.containsClaim("fake") && token.getClaimAsString("fake").equals("1")) {
        fake = true;
      }
      if (request.isFake() == 1) {
        fake = true;
      }
      return fake;
    }
    throw new IllegalArgumentException();
  }
}
