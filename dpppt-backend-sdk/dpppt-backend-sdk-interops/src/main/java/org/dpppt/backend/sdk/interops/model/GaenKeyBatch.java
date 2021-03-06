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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;

public class GaenKeyBatch {
  private List<GaenKeyForInterops> keys = new ArrayList<>();
  private String batchTag;
  private String nextBatchTag;
  private LocalDate date;

  public GaenKeyBatch(LocalDate date) {
    this.date = date;
  }

  public List<GaenKeyForInterops> getKeys() {
    return keys;
  }

  public void setKeys(List<GaenKeyForInterops> keys) {
    this.keys = keys;
  }

  public String getBatchTag() {
    return batchTag;
  }

  public void setBatchTag(String batchTag) {
    this.batchTag = batchTag;
  }

  public String getNextBatchTag() {
    return nextBatchTag;
  }

  public void setNextBatchTag(String nextBatchTag) {
    if ("null".equals(nextBatchTag)
        || (this.batchTag != null && this.batchTag.equals(nextBatchTag))) {
      this.nextBatchTag = null;
    } else {
      this.nextBatchTag = nextBatchTag;
    }
  }

  public LocalDate getDate() {
    return date;
  }

  public boolean isLastBatchForDay() {
    return nextBatchTag == null;
  }

  @Override
  public String toString() {
    return "GaenKeyBatch{"
        + "keyCount="
        + keys.size()
        + ", batchTag='"
        + batchTag
        + '\''
        + ", nextBatchTag='"
        + nextBatchTag
        + '\''
        + ", date="
        + date
        + '}';
  }
}
