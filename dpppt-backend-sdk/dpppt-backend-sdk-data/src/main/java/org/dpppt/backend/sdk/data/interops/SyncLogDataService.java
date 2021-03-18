/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.data.interops;

import java.time.LocalDate;
import org.dpppt.backend.sdk.model.interops.FederationSyncLogEntry;

public interface SyncLogDataService {

  /**
   * returns the latest batch tag for the given `uploadDate` or null if no batches have been
   * downloaded yet
   *
   * @param uploadDate
   * @return
   */
  String getLatestBatchTagForDay(LocalDate uploadDate);

  /**
   * inserts the given log entry
   *
   * @param logEntry
   */
  void insertLogEntry(FederationSyncLogEntry logEntry);
}
