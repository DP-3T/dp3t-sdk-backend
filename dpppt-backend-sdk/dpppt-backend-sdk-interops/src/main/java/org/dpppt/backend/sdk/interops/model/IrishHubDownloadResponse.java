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
public class IrishHubDownloadResponse {

  private String batchTag;
  private List<IrishHubKey> exposures;

  public String getBatchTag() {
    return batchTag;
  }

  public void setBatchTag(String batchTag) {
    this.batchTag = batchTag;
  }

  public List<IrishHubKey> getExposures() {
    return exposures;
  }

  public void setExposures(List<IrishHubKey> exposures) {
    this.exposures = exposures;
  }
}
