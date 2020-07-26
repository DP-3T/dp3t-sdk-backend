package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;

public class RollingStartNumberBeforeRetentionDay implements InsertionFilter {
    private final ValidationUtils validationUtils;
    public RollingStartNumberBeforeRetentionDay(ValidationUtils validationUtils) {
        this.validationUtils = validationUtils;
    }
    @Override
    public List<GaenKey> filter(UTCInstant now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) {
        return content.stream().filter(key -> {
            var timestamp = UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
            if(validationUtils.isBeforeRetention(timestamp, now)){
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }
}