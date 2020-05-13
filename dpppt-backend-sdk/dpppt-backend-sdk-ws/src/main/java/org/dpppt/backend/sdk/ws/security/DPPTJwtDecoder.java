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

import java.security.PublicKey;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

public class DPPTJwtDecoder implements JwtDecoder {
	private final JwtParser parser;
	private OAuth2TokenValidator<Jwt> validator;

	public DPPTJwtDecoder(PublicKey publicKey) {
		parser = Jwts.parserBuilder().setSigningKey(publicKey).build();
	}

	public void setJwtValidator(OAuth2TokenValidator<Jwt> validator) {
		this.validator = validator;
	}

	@Override
	public Jwt decode(String token) throws JwtException {
		try {
			var t = parser.parse(token);

			var headers = t.getHeader();
			var claims = (Claims) t.getBody();
			var iat = claims.getIssuedAt();

			var springJwt = new Jwt(token, iat.toInstant(), claims.getExpiration().toInstant(), headers, claims);

			if (validator != null) {
				var validationResult = validator.validate(springJwt);
				if (validationResult.hasErrors()) {
					String errorMessage = "";
					for (var error : validationResult.getErrors()) {
						errorMessage += error.getDescription() + "\n";
					}
					throw new JwtException(errorMessage);
				}
			}
			return springJwt;
		} catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException ex) {
			throw new JwtException(ex.getMessage());
		}

	}
}