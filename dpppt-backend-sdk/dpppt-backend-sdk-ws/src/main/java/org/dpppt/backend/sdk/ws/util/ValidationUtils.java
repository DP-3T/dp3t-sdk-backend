/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.backend.sdk.ws.util;

import org.dpppt.backend.sdk.utils.UTCInstant;
import java.time.Duration;
import java.util.Base64;

/**
 * Offers a set of methods to validate the incoming requests from the mobile devices.
 */
public class ValidationUtils {
	private final int KEY_LENGTH_BYTES;
	private final Duration retentionPeriod;
	private final Long releaseBucketDuration;

	/**
	 * Initialize the validator with the current parameters in use.
	 *
	 * @param keyLengthBytes how long the exposed keys are, in bytes
	 * @param retentionPeriod period during which the exposed keys are stored, before they are deleted
	 * @param releaseBucketDuration maximum time in milliseconds that exposed keys are hidden before being served, in order to prevent timing attacks
	 */
	public ValidationUtils(int keyLengthBytes, Duration retentionPeriod, Long releaseBucketDuration) {
		this.KEY_LENGTH_BYTES = keyLengthBytes;
		this.retentionPeriod = retentionPeriod;
		this.releaseBucketDuration = releaseBucketDuration;
	}

	/**
	 * Check the validty of a base64 value
	 *
	 * @param value representation of a base64 value
	 * @return if _value_ is a valid representation
	 */
	public boolean isValidBase64Key(String value) {
		try {
			byte[] key = Base64.getDecoder().decode(value);
			if (key.length != KEY_LENGTH_BYTES) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Check if the given date is in the range of [now - retentionPeriod ... now], inclusive
	 *
	 * @param timestamp to verify
	 * @return if the date is in the range
	 */
	public boolean isDateInRange(UTCInstant timestamp, UTCInstant now) {
		var retention = now.minus(retentionPeriod);
		// This should use timestamp.isAfterOrEqual(retention), but this method does not exist.
		// Because _now_ has a resolution of 1 millisecond, this precision is acceptable.
		return timestamp.isAfterEpochMillisOf(retention) && timestamp.isBeforeEpochMillisOf(now);
	}

	/**
	 * Check if the given timestamp is a valid key date: Must be midnight UTC.
	 * 
	 * @param keyDate as a UTCInstant
	 * @return if keyDate represents midnight UTC
	 */
	public boolean isValidKeyDate(UTCInstant keyDate) {
		return keyDate.isMidnight();
	}

	/**
	 * Check if the given batchReleaseTime is the beginning of a batch, and if it is between
	 * [now - retentionPeriod ... now], inclusive.
	 *
	 * @param batchReleaseTime in milliseconds since Unix epoch (1970-01-01)
	 * @return if batchReleaseTime is in range
	 * @throws BadBatchReleaseTimeException if batchReleaseTime is not on a batch boundary
	 */
	public boolean isValidBatchReleaseTime(UTCInstant batchReleaseTime, UTCInstant now) throws BadBatchReleaseTimeException {
		if (batchReleaseTime.getTimestamp() % releaseBucketDuration != 0) {
			throw new BadBatchReleaseTimeException();
		}
		return this.isDateInRange(batchReleaseTime, now);
	}

	public class BadBatchReleaseTimeException extends Exception {

		private static final long serialVersionUID = 618376703047108588L;

	}

}