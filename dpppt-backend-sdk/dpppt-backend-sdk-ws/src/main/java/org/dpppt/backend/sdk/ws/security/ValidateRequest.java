package org.dpppt.backend.sdk.ws.security;

import org.joda.time.DateTime;

public interface ValidateRequest {
    public boolean isValid(Object authObject);
    public String getOnset(Object authObject);
}