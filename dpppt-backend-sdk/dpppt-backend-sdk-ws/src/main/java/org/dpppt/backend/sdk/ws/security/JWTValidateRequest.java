package org.dpppt.backend.sdk.ws.security;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.security.oauth2.jwt.Jwt;

public class JWTValidateRequest implements ValidateRequest<Jwt> {

    private static final DateTimeFormatter DAY_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd")
    .withZone(DateTimeZone.UTC);

    @Override
    public boolean isValid(Jwt authObject) {
        return authObject.containsClaim("scope") && authObject.getClaim("scope").equals("exposed");
    }

    @Override
    public String getOnset(Jwt authObject) {
        return authObject.getClaim("onset");
    }

}