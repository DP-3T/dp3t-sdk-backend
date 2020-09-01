/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.data.gaen;

import java.time.Duration;
import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.utils.UTCInstant;

public interface GAENDataService {

  /**
   * Upserts (Update or Inserts) the given list of exposed keys
   *
   * @param keys the list of exposed keys to upsert
   * @param now time of the request
   */
  void upsertExposees(List<GaenKey> keys, UTCInstant now);

  /**
   * Upserts (Update or Inserts) the given list of exposed keys, with delayed release of same day
   * TEKs
   *
   * @param keys the list of exposed keys to upsert
   * @param delayedReceivedAt the timestamp to use for the delayed release (if null use now rounded
   *     to next bucket)
   * @param now time of the request
   */
  void upsertExposeesDelayed(List<GaenKey> keys, UTCInstant delayedReceivedAt, UTCInstant now);

  /**
   * Returns the maximum id of the stored exposed entries for the given batch.
   *
   * @param keyDate must be midnight UTC
   * @param publishedAfter when publication should start
   * @param publishedUntil last publication
   * @param now the start of the query
   * @return the maximum id of the stored exposed entries for the given batch
   */
  int getMaxExposedIdForKeyDate(
      UTCInstant keyDate, UTCInstant publishedAfter, UTCInstant publishedUntil, UTCInstant now);

  /**
   * Returns all exposeed keys for the given batch.
   *
   * @param keyDate must be midnight UTC
   * @param publishedAfter when publication should start
   * @param publishedUntil last publication
   * @param now the start of the query
   * @return all exposeed keys for the given batch
   */
  List<GaenKey> getSortedExposedForKeyDate(
      UTCInstant keyDate, UTCInstant publishedAfter, UTCInstant publishedUntil, UTCInstant now);

  /**
   * deletes entries older than retentionperiod
   *
   * @param retentionPeriod in milliseconds
   */
  void cleanDB(Duration retentionPeriod);
}
