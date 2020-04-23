/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.security;

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
	public String getOnset(Object authObject, Object others) {
		if (authObject instanceof Jwt) {
			Jwt token = (Jwt) authObject;
			return token.getClaim("onset");
		}
		return "";
	}

}