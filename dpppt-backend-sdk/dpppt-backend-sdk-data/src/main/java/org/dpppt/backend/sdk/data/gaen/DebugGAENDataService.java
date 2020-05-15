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

import java.util.List;
import java.util.Map;

import org.dpppt.backend.sdk.model.gaen.GaenKey;

public interface DebugGAENDataService {

	/**
	 * Upserts the given list of exposed keys in the debug store
	 * 
	 * @param device name
	 * @param key    the list of exposed keys to upsert
	 */
	void upsertExposees(String deviceName, List<GaenKey> keys);

	/**
	 * Returns all exposeed keys for the given batch from the debug store.
	 * 
	 * @param batchReleaseTime
	 * @param batchLength
	 * @return
	 */
	Map<String, List<GaenKey>> getSortedExposedForBatchReleaseTime(Long batchReleaseTime, long batchLength);

}
