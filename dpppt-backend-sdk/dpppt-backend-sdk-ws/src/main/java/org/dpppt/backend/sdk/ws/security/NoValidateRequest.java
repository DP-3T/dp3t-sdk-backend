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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;

public class NoValidateRequest implements ValidateRequest {

	@Override
	public boolean isValid(Object authObject) {
		return true;
	}

	@Override
	public long validateKeyDate(Object authObject, Object others) throws ClaimIsBeforeOnsetException,InvalidDateException {
		if (others instanceof ExposeeRequest) {
			ExposeeRequest request = ((ExposeeRequest) others);
			if (request.getKeyDate() < OffsetDateTime.now().minusDays(21).toInstant().toEpochMilli()) {
				throw new InvalidDateException();
			} else if (request.getKeyDate() > OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant()
					.toEpochMilli()) {
				throw new InvalidDateException();
			}
			return request.getKeyDate();
		}
		if (others instanceof GaenKey) {
			GaenKey key = ((GaenKey) others);
			var requestDate = Duration.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
			if (requestDate.toMillis() < OffsetDateTime.now().minusDays(21).toInstant().toEpochMilli()) {
				throw new InvalidDateException();
			} else if (requestDate.toMillis() > OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant()
					.toEpochMilli()) {
				throw new InvalidDateException();
			}
			return requestDate.toMillis();
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