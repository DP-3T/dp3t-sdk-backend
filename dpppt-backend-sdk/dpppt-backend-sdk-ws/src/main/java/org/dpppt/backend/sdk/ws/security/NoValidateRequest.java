/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.security;

import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.utils.UTCInstant;

public class NoValidateRequest implements ValidateRequest {

	@Override
	public boolean isValid(Object authObject) {
		return true;
	}

	@Override
	public long validateKeyDate(UTCInstant utcNow, Object authObject, Object others) throws ClaimIsBeforeOnsetException,InvalidDateException {
		if (others instanceof ExposeeRequest) {
			ExposeeRequest request = ((ExposeeRequest) others);
			if (request.getKeyDate() < utcNow.minusDays(21).getTimestamp()) {
				throw new InvalidDateException();
			} else if (request.getKeyDate() > utcNow.getTimestamp()) {
				throw new InvalidDateException();
			}
			return request.getKeyDate();
		}
		if (others instanceof GaenKey) {
			GaenKey key = ((GaenKey) others);
			var requestDate = UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
			if (requestDate.isBeforeExact(utcNow.minusDays(21))) {
				throw new InvalidDateException();
			} else if (requestDate.isAfterExact(utcNow)) {
				throw new InvalidDateException();
			}
			return requestDate.getTimestamp();
		}
		throw new IllegalArgumentException();
	}

	@Override
	public boolean isFakeRequest(Object authObject, Object others) {
		if (others instanceof ExposeeRequest) {
			ExposeeRequest request = ((ExposeeRequest) others);
			return request.isFake() == 1;
		}
		if (others instanceof GaenKey) {
			GaenKey request = ((GaenKey) others);
			return request.getFake() == 1;
		}
		throw new IllegalArgumentException();
	}

}