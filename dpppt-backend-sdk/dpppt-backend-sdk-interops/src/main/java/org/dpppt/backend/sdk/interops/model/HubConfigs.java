/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.interops.model;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "interops.hubs")
public class HubConfigs {
  private List<EfgsGatewayConfig> efgsGateways;

  public List<EfgsGatewayConfig> getEfgsGateways() {
    return efgsGateways;
  }

  public void setEfgsGateways(List<EfgsGatewayConfig> efgsGateways) {
    this.efgsGateways = efgsGateways;
  }
}
