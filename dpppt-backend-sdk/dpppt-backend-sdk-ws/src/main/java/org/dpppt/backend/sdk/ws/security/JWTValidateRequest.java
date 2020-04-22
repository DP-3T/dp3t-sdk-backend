package org.dpppt.backend.sdk.ws.security;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.security.oauth2.jwt.Jwt;

public class JWTValidateRequest implements ValidateRequest{

    private static final DateTimeFormatter DAY_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd")
    .withZone(DateTimeZone.UTC);

    @Override
    public boolean isValid(Object authObject) {
        if(authObject instanceof Jwt) {
            Jwt token = (Jwt)authObject;
            return token.containsClaim("scope") && token.getClaim("scope").equals("exposed");
        }
       return false;
    }

    @Override
    public String getOnset(Object authObject, Object others) {
        if(authObject instanceof Jwt) {
            Jwt token = (Jwt)authObject;
            return token.getClaim("onset");
        }
        return "";
    }

}