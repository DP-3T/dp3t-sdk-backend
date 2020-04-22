package org.dpppt.backend.sdk.ws.security;

import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;


public class JWTValidator implements OAuth2TokenValidator<Jwt> {

    public static final String UUID_CLAIM = "sub";


    private DPPPTDataService dataService;
    public JWTValidator(DPPPTDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if(this.dataService.checkAndInsertPublishUUID(token.getClaim(UUID_CLAIM))) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_SCOPE));
    }

}