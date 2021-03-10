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
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;

public interface GaenDataService {

  /**
   * Upserts (Update or Inserts) the given key received from interops synchronization.
   *
   * @param key the exposed key to upsert
   * @param now time of the sync
   * @param origin the origin or the key
   */
  void upsertExposeeFromInterops(GaenKey key, UTCInstant now, String origin);

  /**
   * Upserts (Update or Inserts) the given list of exposed keys
   *
   * @param keys the list of exposed keys to upsert
   * @param now time of the request
   * @param withFederationGateway if set to true, the given keys are stored such that they can be
   *     shared with other federation gateways.
   */
  void upsertExposees(List<GaenKey> keys, UTCInstant now, boolean withFederationGateway);

  /**
   * Upserts (Update or Inserts) the given list of exposed keys, with delayed release of same day
   * TEKs
   *
   * @param keys the list of exposed keys to upsert
   * @param delayedReceivedAt the timestamp to use for the delayed release (if null use now rounded
   *     to next bucket)
   * @param now time of the request
   * @param withFederationGateway if set to true, the given keys are stored such that they can be
   *     shared with other federation gateways.
   */
  void upsertExposeesDelayed(
      List<GaenKey> keys,
      UTCInstant delayedReceivedAt,
      UTCInstant now,
      boolean withFederationGateway);

  /**
   * Returns all exposeed keys for the given batch, where a batch is parametrized with keyDate (for
   * which day was the key used) publishedAfter/publishedUntil (when was the key published) and now
   * (has the key expired or not, based on rollingStartNumber and rollingPeriod).
   *
   * @param keyDate must be midnight UTC
   * @param publishedAfter when publication should start
   * @param publishedUntil last publication
   * @param now the start of the query
   * @param withFederationGateway if set to true, the given keys are stored such that they can be
   *     shared with other federation gateways.
   * @return all exposeed keys for the given batch
   */
  List<GaenKey> getSortedExposedForKeyDate(
      UTCInstant keyDate,
      UTCInstant publishedAfter,
      UTCInstant publishedUntil,
      UTCInstant now,
      boolean withFederationGateway);

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
   * @param withFederationGateway If set to true, all keys from federation gateways are returned in
   *     the result. Otherwise only keys from the origin country.
   * @return
   */
  List<GaenKey> getSortedExposedSince(
      UTCInstant keysSince, UTCInstant now, boolean withFederationGateway);

  /**
   * Returns all exposed keys since keySince, with origin but only from our own origin.
   *
   * @param keysSince
   * @param now
   * @return
   */
  List<GaenKeyForInterops> getSortedExposedSinceForInteropsFromOrigin(
      UTCInstant keysSince, UTCInstant now);

  /**
   * Returns all exposed keys with our own origin that are to be uploaded to the federation gateway,
   * and haven't been uploaded yet.
   *
   * @return
   */
  List<GaenKeyForInterops> getExposedForEfgsUpload();

  /**
   * sets the batch tag for the given keys
   *
   * @param uploadedKeys
   * @param batchTag
   */
  void setBatchTagForKeys(List<GaenKeyForInterops> uploadedKeys, String batchTag);
}
