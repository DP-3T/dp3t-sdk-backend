/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.security;

import java.time.LocalDate;
import java.time.ZoneOffset;
import org.dpppt.backend.sdk.model.ExposeeRequest;
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
	public long getKeyDate(Object authObject, Object others) {
		if (authObject instanceof Jwt) {
			Jwt token = (Jwt) authObject;
			
			long jwtKeyDate = LocalDate.parse(token.getClaim("onset")).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
			if (others instanceof ExposeeRequest) {
				ExposeeRequest request = (ExposeeRequest) others;
				long maxKeyDate = Math.max(jwtKeyDate, request.getKeyDate());
				if (maxKeyDate > System.currentTimeMillis()) {
					// the maximum key date is the current day.
					maxKeyDate = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
				}
				jwtKeyDate = maxKeyDate;
			}
			return jwtKeyDate;
		}
		throw new IllegalArgumentException();
	}

}