/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.interops.config.configbeans;

public class ActuatorSecurityConfig {
  private final String username;
  private final String password;

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public ActuatorSecurityConfig(String username, String password) {
    this.username = username;
    this.password = password;
  }
}
