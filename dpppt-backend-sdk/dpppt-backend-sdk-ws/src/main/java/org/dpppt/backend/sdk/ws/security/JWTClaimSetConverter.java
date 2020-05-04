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

import java.util.Collections;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;

public class JWTClaimSetConverter implements Converter<Map<String, Object>, Map<String, Object>> {

	private final MappedJwtClaimSetConverter delegate = MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap());

	public Map<String, Object> convert(Map<String, Object> claims) {
		Map<String, Object> convertedClaims = this.delegate.convert(claims);
		String onset = convertedClaims.get("onset") != null ? (String) convertedClaims.get("onset") : "-";

		convertedClaims.put("onset", onset);

		return convertedClaims;
	}
}