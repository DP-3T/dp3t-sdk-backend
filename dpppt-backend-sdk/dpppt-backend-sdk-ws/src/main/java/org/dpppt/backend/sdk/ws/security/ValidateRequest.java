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

import org.dpppt.backend.sdk.utils.UTCInstant;

public interface ValidateRequest {

  public boolean isValid(Object authObject);

  // authObject is the Principal, given from Springboot
  // others can be any object (currently it is the ExposeeRequest, since we want
  // to allow no auth without the jwt profile)
  public long getKeyDate(UTCInstant now, Object authObject, Object others)
      throws InvalidDateException;

  public boolean isFakeRequest(Object authObject, Object others);

  public class InvalidDateException extends Exception {

    private static final long serialVersionUID = 5886601055826066148L;
  }
}
