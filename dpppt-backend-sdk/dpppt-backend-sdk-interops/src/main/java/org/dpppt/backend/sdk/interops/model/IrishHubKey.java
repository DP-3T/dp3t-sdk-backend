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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IrishHubKey {

  private String keyData;
  private Integer rollingStartNumber;
  private Integer transmissionRiskLevel;
  private Integer rollingPeriod;
  private String origin;
  private List<String> regions;

  public String getKeyData() {
    return keyData;
  }

  public void setKeyData(String keyData) {
    this.keyData = keyData;
  }

  public Integer getRollingStartNumber() {
    return rollingStartNumber;
  }

  public void setRollingStartNumber(Integer rollingStartNumber) {
    this.rollingStartNumber = rollingStartNumber;
  }

  public Integer getTransmissionRiskLevel() {
    return transmissionRiskLevel;
  }

  public void setTransmissionRiskLevel(Integer transmissionRiskLevel) {
    this.transmissionRiskLevel = transmissionRiskLevel;
  }

  public Integer getRollingPeriod() {
    return rollingPeriod;
  }

  public void setRollingPeriod(Integer rollingPeriod) {
    this.rollingPeriod = rollingPeriod;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public List<String> getRegions() {
    return regions;
  }

  public void setRegions(List<String> regions) {
    this.regions = regions;
  }
}
