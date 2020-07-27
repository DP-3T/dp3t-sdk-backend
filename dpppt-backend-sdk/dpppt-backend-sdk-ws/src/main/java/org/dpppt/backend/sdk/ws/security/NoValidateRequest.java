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

import java.time.Duration;

import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;

public class NoValidateRequest implements ValidateRequest {
	private final ValidationUtils validationUtils;
	public NoValidateRequest(ValidationUtils validationUtils){
		this.validationUtils = validationUtils;
	}
	@Override
	public boolean isValid(Object authObject) {
		return true;
	}

	@Override
	public long getKeyDate(UTCInstant now, Object authObject, Object others) throws InvalidDateException {
		if (others instanceof ExposeeRequest) {
			ExposeeRequest request = ((ExposeeRequest) others);
			var requestKeyDate = new UTCInstant(request.getKeyDate());
			checkDateIsInBounds(requestKeyDate, now);
			return request.getKeyDate();
		}
		if (others instanceof GaenKey) {
			GaenKey key = ((GaenKey) others);
			var requestKeyDate = UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
			checkDateIsInBounds(requestKeyDate, now);
			return requestKeyDate.getTimestamp();
		}
		throw new IllegalArgumentException();
	}

	private void checkDateIsInBounds(UTCInstant requestKeyDate, UTCInstant now) throws InvalidDateException {
		if (!validationUtils.isDateInRange(requestKeyDate, now)) {
			throw new InvalidDateException();
		}
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