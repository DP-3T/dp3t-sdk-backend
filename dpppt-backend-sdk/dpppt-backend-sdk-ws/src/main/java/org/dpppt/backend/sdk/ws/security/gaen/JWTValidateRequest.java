/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.security.gaen;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.springframework.security.oauth2.jwt.Jwt;

public class JWTValidateRequest implements ValidateRequest {

	@Override
	public boolean isValid(Object authObject) {
		if (authObject instanceof Jwt) {
			Jwt token = (Jwt) authObject;
			return token.containsClaim("scope") && token.getClaim("scope").equals("exposed");
		}
		return false;
	}

	@Override
	public long getKeyDate(UTCInstant utcNow, Object authObject, Object others) throws InvalidDateException {
		if (authObject instanceof Jwt) {
			Jwt token = (Jwt) authObject;
			var jwtKeyDate = UTCInstant.parseDate(token.getClaim("onset"));
			if (others instanceof GaenKey) {
                GaenKey request = (GaenKey) others;
                var keyDate = UTCInstant.of(request.getRollingStartNumber(), GaenUnit.TenMinutes);
				if (keyDate.isAfterExact(utcNow)) {
					throw new InvalidDateException();
				} else if (keyDate.isBeforeExact(jwtKeyDate)) {
					throw new InvalidDateException();
				} 
				//TODO: fix 14 to retentionPeriod Constant
				else if(keyDate.isBeforeExact(utcNow.minusDays(14))) {
					throw new InvalidDateException();
				}
				jwtKeyDate = keyDate;
			}
			return jwtKeyDate.getTimestamp();
		}
		throw new IllegalArgumentException();
	}

	@Override
	public boolean isFakeRequest(Object authObject, Object others) {
		if (authObject instanceof Jwt && others instanceof GaenKey) {
			Jwt token = (Jwt) authObject;
			GaenKey request = (GaenKey) others;
			boolean fake = false;
			if (token.containsClaim("fake") && token.getClaimAsString("fake").equals("1")) {
				fake = true;
			}
			if (request.getFake() == 1) {
				fake = true;
			}
			return fake;
		}
		throw new IllegalArgumentException();
	}

}