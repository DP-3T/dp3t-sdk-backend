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

public class IrishHubUploadResponse {
  private String batchTag;
  private Integer insertedExposures;

  public String getBatchTag() {
    return batchTag;
  }

  public void setBatchTag(String batchTag) {
    this.batchTag = batchTag;
  }

  public Integer getInsertedExposures() {
    return insertedExposures;
  }

  public void setInsertedExposures(Integer insertedExposures) {
    this.insertedExposures = insertedExposures;
  }
}
