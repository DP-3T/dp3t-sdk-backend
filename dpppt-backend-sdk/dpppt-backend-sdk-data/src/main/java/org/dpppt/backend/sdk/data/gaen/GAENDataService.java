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

public interface GAENDataService {

	/**
	 * Upserts the given list of exposed keys
	 * 
	 * @param key the list of exposed keys to upsert
	 */
	void upsertExposees(List<GaenKey> keys);

	/**
	 * Returns the maximum id of the stored exposed entries for the given batch.
	 * 
	 * @param batchReleaseTime
	 * @param batchLength
	 * @return
	 */
	int getMaxExposedIdForBatchReleaseTime(Long batchReleaseTime, long batchLength);

	/**
	 * Returns all exposeed keys for the given batch.
	 * 
	 * @param batchReleaseTime
	 * @param batchLength
	 * @return
	 */
	List<GaenKey> getSortedExposedForBatchReleaseTime(Long batchReleaseTime, long batchLength);

	/**
	 * deletes entries older than retentionperiod
	 * 
	 * @param retentionPeriod
	 */
	void cleanDB(Duration retentionPeriod);
}
