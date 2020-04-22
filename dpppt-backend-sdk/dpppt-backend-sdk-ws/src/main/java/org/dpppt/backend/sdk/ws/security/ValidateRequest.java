package org.dpppt.backend.sdk.ws.security;

import org.joda.time.DateTime;

public interface ValidateRequest {
    public boolean isValid(Object authObject);
    // authObject is the Principal, given from Springboot
    // others can be any object (currently it is the ExposeeRequest, since we want to allow no auth without the jwt profile)
    public String getOnset(Object authObject, Object others);
}