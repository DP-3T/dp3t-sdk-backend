/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.data;

import java.time.Duration;
import java.util.List;

import org.dpppt.backend.sdk.model.Exposee;

public interface DPPPTDataService {

	/**
	 * Upserts the given exposee
	 * 
	 * @param exposee   the exposee to upsert
	 * @param appSource the app name
	 */
	void upsertExposee(Exposee exposee, String appSource);

	/**
	 * Upserts the given exposees (if keys cannot be derived from one master key)
	 * 
	 * @param exposeex  the list of exposees to upsert
	 * @param appSource the app name
	 */
	void upsertExposees(List<Exposee> exposees, String appSource);

	/**
	 * Returns the maximum id of the stored exposed entries fo the given batch.
	 * 
	 * @param batchReleaseTime
	 * @param batchLength
	 * @return
	 */
	int getMaxExposedIdForBatchReleaseTime(long batchReleaseTime, long batchLength);

	/**
	 * Returns all exposees for the given batch.
	 * 
	 * @param batchReleaseTime
	 * @param batchLength
	 * @return
	 */
	List<Exposee> getSortedExposedForBatchReleaseTime(long batchReleaseTime, long batchLength);

	/**
	 * deletes entries older than retentionperiod
	 * 
	 * @param retentionPeriod
	 */
	void cleanDB(Duration retentionPeriod);

}
