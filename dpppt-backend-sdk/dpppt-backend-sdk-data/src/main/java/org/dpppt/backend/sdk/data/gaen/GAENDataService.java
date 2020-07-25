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
	 */
	void upsertExposees(List<GaenKey> keys);

	/**
	 * Upserts (Update or Inserts) the given list of exposed keys, with delayed release of same day TEKs
	 * 
	 * @param keys the list of exposed keys to upsert
	 * @param delayedReceivedAt the timestamp to use for the delayed release (if null use now rounded to next bucket)
	 */
	void upsertExposeesDelayed(List<GaenKey> keys, UTCInstant delayedReceivedAt);

	/**
	 * Returns the maximum id of the stored exposed entries for the given batch.
	 * 
	 * @param keyDate in milliseconds since Unix epoch (1970-01-01)
	 * @param publishedAfter in milliseconds since Unix epoch
	 * @param publishedUntil in milliseconds since Unix epoch
	 * @return the maximum id of the stored exposed entries for the given batch
	 */
	int getMaxExposedIdForKeyDate(Long keyDate, Long publishedAfter, Long publishedUntil);

	/**
	 * Returns all exposeed keys for the given batch.
	 *
	 * @param keyDate in milliseconds since Unix epoch (1970-01-01)
	 * @param publishedAfter in milliseconds since Unix epoch
	 * @param publishedUntil in milliseconds since Unix epoch
	 * @return all exposeed keys for the given batch
	 */
	List<GaenKey> getSortedExposedForKeyDate(Long keyDate, Long publishedAfter, Long publishedUntil);

	/**
	 * deletes entries older than retentionperiod
	 * 
	 * @param retentionPeriod in milliseconds
	 */
	void cleanDB(Duration retentionPeriod);
}
