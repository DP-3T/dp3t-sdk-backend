package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.ClaimIsBeforeOnsetException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.InvalidDateException;

public class KeysNotMatchingJWTFilter implements InsertionFilter {
    private final ValidateRequest validateRequest;
    public KeysNotMatchingJWTFilter(ValidateRequest validateRequest){
        this.validateRequest = validateRequest;
    }

    @Override
    public List<GaenKey> filter(long now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion,
            Object principal) {
        return content.stream().filter((key) -> {
            try {
                validateRequest.validateKeyDate(principal, key);
                return true;
            }
            catch(InvalidDateException | ClaimIsBeforeOnsetException es) {
                return false;
            }
        }).collect(Collectors.toList());
    }
    
}