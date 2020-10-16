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
   * Upserts (Update or Inserts) the given key received from interops synchronization.
   *
   * @param key the exposed key to upsert
   * @param now time of the sync
   * @param origin the origin or the key
   * @param visitedCountries the countries the key visited
   */
  void upsertExposeeFromInterops(
      GaenKey key, UTCInstant now, String origin, List<String> visitedCountries);

  /**
   * Upserts (Update or Inserts) the given list of exposed keys
   *
   * @param keys the list of exposed keys to upsert
   * @param now time of the request
   * @param international if set to true, the given keys are stored such that they have visited all
   *     configured countries.
   */
  void upsertExposees(List<GaenKey> keys, UTCInstant now, boolean international);

  /**
   * Upserts (Update or Inserts) the given list of exposed keys, with delayed release of same day
   * TEKs
   *
   * @param keys the list of exposed keys to upsert
   * @param delayedReceivedAt the timestamp to use for the delayed release (if null use now rounded
   *     to next bucket)
   * @param now time of the request
   * @param international if set to true, the given keys are stored such that they have visited all
   *     configured countries.
   */
  void upsertExposeesDelayed(
      List<GaenKey> keys, UTCInstant delayedReceivedAt, UTCInstant now, boolean international);

  /**
   * Returns all exposeed keys for the given batch, where a batch is parametrized with keyDate (for
   * which day was the key used) publishedAfter/publishedUntil (when was the key published) and now
   * (has the key expired or not, based on rollingStartNumber and rollingPeriod).
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

  /**
   * Returns all exposed keys since keySince.
   *
   * @param keysSince
   * @param now
   * @param includeAllInternationalKeys If set to true, all international keys are returned in the
   *     result. Otherwise only keys from the origin country.
   * @return
   */
  List<GaenKey> getSortedExposedSince(
      UTCInstant keysSince, UTCInstant now, boolean includeAllInternationalKeys);
}
