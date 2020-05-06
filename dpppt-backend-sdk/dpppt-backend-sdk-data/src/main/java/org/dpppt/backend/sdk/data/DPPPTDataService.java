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

import java.time.OffsetDateTime;
import java.util.List;

import org.dpppt.backend.sdk.model.Exposee;


public interface DPPPTDataService {

	/**
	 * Upserts the given exposee
	 * 
	 * @param exposee the exposee to upsert
	 * @param appSource the app name
	 */
	void upsertExposee(Exposee exposee, String appSource);

	/**
	 * Upserts the given exposees (if keys cannot be derived from one master key)
	 * 
	 * @param exposeex the list of exposees to upsert
	 * @param appSource the app name
	 */
	void upsertExposees(List<Exposee> exposees, String appSource);

	/**
	 * Returns all exposees for the given day [day: 00:00, day+1: 00:00] ordered by id
	 * 
	 * @param day the day for which exposees are requested
	 * @return exposee list
	 */
	List<Exposee> getSortedExposedForDay(OffsetDateTime day);

	/**
	 * Returns the maximum id of the stored exposed entries for the given day date
	 * 
	 * @param day the day for which id is required
	 * 
	 * @return the max id or 0
	 */
	Integer getMaxExposedIdForDay(OffsetDateTime day);

	/**
	 * Checks and inserts a publish uuid.
	 * 
	 * @param uuid
	 * @return return true if the uuid has been inserted. if the uuid is not valid,
	 *         returns false.
	 */
	boolean checkAndInsertPublishUUID(String uuid);

	/**
	 * Returns the maximum id of the stored exposed entries fo the given batch.
	 * 
	 * @param batchReleaseTime
	 * @param batchLength
	 * @return
	 */
	int getMaxExposedIdForBatchReleaseTime(Long batchReleaseTime, long batchLength);

	/**
	 * Returns all exposees for the given batch.
	 * 
	 * @param batchReleaseTime
	 * @param batchLength
	 * @return
	 */
	List<Exposee> getSortedExposedForBatchReleaseTime(Long batchReleaseTime, long batchLength);

	/**
	 * deletes entries older than retentionDays
	 * @param retentionDays
	 */
	void cleanDB(int retentionDays);

}
