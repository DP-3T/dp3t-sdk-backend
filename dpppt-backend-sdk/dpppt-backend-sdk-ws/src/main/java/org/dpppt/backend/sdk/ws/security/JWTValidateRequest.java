/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.security;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
	public long getKeyDate(Object authObject, Object others) throws InvalidDateException {
		if (authObject instanceof Jwt) {
			Jwt token = (Jwt) authObject;
			long jwtKeyDate = LocalDate.parse(token.getClaim("onset")).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
			if (others instanceof ExposeeRequest) {
				ExposeeRequest request = (ExposeeRequest) others;
				if (request.getKeyDate() > System.currentTimeMillis()) {
					throw new InvalidDateException();
				} else if (request.getKeyDate() < jwtKeyDate) {
					throw new InvalidDateException();
				} 
				else if(request.getKeyDate() < OffsetDateTime.now().minusDays(21).toInstant().toEpochMilli()) {
					throw new InvalidDateException();
				}
				jwtKeyDate = request.getKeyDate();
			}
			return jwtKeyDate;
		}
		throw new IllegalArgumentException();
	}

	@Override
	public boolean isFakeRequest(Object authObject, Object others) {
		if (authObject instanceof Jwt && others instanceof ExposeeRequest) {
			Jwt token = (Jwt) authObject;
			ExposeeRequest request = (ExposeeRequest) others;
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