package org.dpppt.backend.sdk.ws.security;

import org.joda.time.DateTime;

public interface ValidateRequest<T> {
    public boolean isValid(T authObject);
    public String getOnset(T authObject);
}