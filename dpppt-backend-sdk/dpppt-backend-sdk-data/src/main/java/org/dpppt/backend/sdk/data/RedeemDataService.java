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

public interface RedeemDataService {

	/**
	 * Checks and inserts a publish uuid.
	 * 
	 * @param uuid
	 * @return return true if the uuid has been inserted. if the uuid is not valid,
	 *         returns false.
	 */
	boolean checkAndInsertPublishUUID(String uuid);

	/**
	 * Clean up db and remove entries older than the retention days.
	 * 
	 * @param retentionPeriod
	 */
	void cleanDB(Duration retentionPeriod);
}
