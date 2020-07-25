package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;

public class NoBase64Filter implements InsertionFilter {
    private final ValidationUtils validationUtils;
    public NoBase64Filter(ValidationUtils validationUtils) {
        this.validationUtils = validationUtils;
    }
    @Override
    public List<GaenKey> filter(long now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) {
        return content.stream().filter(key -> validationUtils.isValidBase64Key(key.getKeyData())).collect(Collectors.toList());
    }
    
}