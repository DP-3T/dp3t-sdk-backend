package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.ClaimIsBeforeOnsetException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.InvalidDateException;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.DelayedKeyDateClaimIsWrong;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.DelayedKeyDateIsInvalid;

public class KeysNotMatchingJWTFilter implements InsertionFilter {
    private final ValidateRequest validateRequest;
    private final ValidationUtils validationUtils;
    public KeysNotMatchingJWTFilter(ValidateRequest validateRequest, ValidationUtils utils){
        this.validateRequest = validateRequest;
        this.validationUtils = utils;
    }

    @Override
    public List<GaenKey> filter(UTCInstant now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion,
            Object principal) {
        return content.stream().filter(key -> {
            try {
                validationUtils.checkForDelayedKeyDateClaim(principal, key);
                var delayedKeyDate = UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
                return isValidDelayedKeyDate(now, delayedKeyDate);
               
            }
            catch(DelayedKeyDateClaimIsWrong ex) {
                return isValidKeyDate(key, principal, now);
            }
           
        }).collect(Collectors.toList());
    }

    private boolean isValidKeyDate(GaenKey key, Object principal, UTCInstant now) {
        try {
            validateRequest.validateKeyDate(now, principal, key);
            return true;
        }
        catch(InvalidDateException | ClaimIsBeforeOnsetException es) {
            return false;
        }
    }
    private boolean isValidDelayedKeyDate(UTCInstant now, UTCInstant delayedKeyDate) {
        try {
            validationUtils.validateDelayedKeyDate(now, delayedKeyDate);
            return true;
        }
        catch(DelayedKeyDateIsInvalid ex){
            return false;
        }
    }
    
}