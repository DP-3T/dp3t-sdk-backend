/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.security;

import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.security.oauth2.jwt.Jwt;

public class JWTValidateRequest implements ValidateRequest {

	private static final DateTimeFormatter DAY_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd")
			.withZone(DateTimeZone.UTC);

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
			long jwtKeyDate = DAY_DATE_FORMATTER.parseMillis(token.getClaim("onset"));
			if (others instanceof ExposeeRequest) {
				ExposeeRequest request = (ExposeeRequest) others;
				long maxKeyDate = Math.max(jwtKeyDate, request.getKeyDate());
				if (maxKeyDate > System.currentTimeMillis()) {
					// the maximum key date is the current day.
					maxKeyDate = DateTime.now().withZone(DateTimeZone.UTC).withTimeAtStartOfDay().getMillis();
				}
				jwtKeyDate = maxKeyDate;
			}
			return jwtKeyDate;
		}
		throw new IllegalArgumentException();
	}

}