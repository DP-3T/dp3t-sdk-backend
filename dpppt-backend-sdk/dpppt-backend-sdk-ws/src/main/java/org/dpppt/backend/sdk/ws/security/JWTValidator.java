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
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;


public class JWTValidator implements OAuth2TokenValidator<Jwt> {

    public static final String UUID_CLAIM = "jti";


    private DPPPTDataService dataService;
    private Duration maxJwtValidity;

    public JWTValidator(DPPPTDataService dataService, Duration maxJwtValidity) {
        this.dataService = dataService;
        this.maxJwtValidity = maxJwtValidity;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if(token.containsClaim("fake") && token.getClaimAsString("fake").equals("1")){
            //it is a fakte token, but we still assume it is valid
            return OAuth2TokenValidatorResult.success();
        }
        if (token.getExpiresAt() == null || Instant.now().plus(maxJwtValidity).isBefore(token.getExpiresAt())) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        }
        if(token.containsClaim(UUID_CLAIM) && this.dataService.checkAndInsertPublishUUID(token.getClaim(UUID_CLAIM))) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE));
    }

}