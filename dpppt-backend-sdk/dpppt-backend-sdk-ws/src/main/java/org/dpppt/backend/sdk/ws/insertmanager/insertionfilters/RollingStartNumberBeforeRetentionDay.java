package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class RollingStartNumberBeforeRetentionDay implements InsertionFilter {
    @Autowired
    ValidationUtils validationUtils;
    @Override
    public List<GaenKey> filter(long now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion) {
        return content.stream().filter(key -> {
            var rp = key.getRollingStartNumber();
            var timestamp = OffsetDateTime.ofInstant(Instant.ofEpochMilli(rp * GaenUnit.TenMinutes.getDuration().toMillis()), ZoneOffset.UTC);
            if(validationUtils.isBeforeRetention(timestamp)){
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }
}