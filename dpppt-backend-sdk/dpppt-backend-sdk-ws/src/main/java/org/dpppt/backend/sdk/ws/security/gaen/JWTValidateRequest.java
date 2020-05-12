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

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.util.GaenUnit;
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
	public long getKeyDate(Object authObject, Object others) throws InvalidDateException {
		if (authObject instanceof Jwt) {
			Jwt token = (Jwt) authObject;
			long jwtKeyDate = LocalDate.parse(token.getClaim("onset")).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
			if (others instanceof GaenKey) {
                GaenKey request = (GaenKey) others;
                var keyDate = Duration.of(request.getRollingStartNumber(), GaenUnit.TenMinutes);
				if (keyDate.toMillis() > System.currentTimeMillis()) {
					throw new InvalidDateException();
				} else if (keyDate.toMillis() < jwtKeyDate) {
					throw new InvalidDateException();
				} 
				else if(keyDate.toMillis() < OffsetDateTime.now().minusDays(21).toInstant().toEpochMilli()) {
					throw new InvalidDateException();
				}
				jwtKeyDate = keyDate.toMillis();
			}
			return jwtKeyDate;
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
			if (request.isFake() == 1) {
				fake = true;
			}
			return fake;
		}
		throw new IllegalArgumentException();
	}

}