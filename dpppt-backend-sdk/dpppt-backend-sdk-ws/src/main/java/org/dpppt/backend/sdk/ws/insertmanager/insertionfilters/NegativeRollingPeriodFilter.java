package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

public class NegativeRollingPeriodFilter implements InsertionFilter {

    @Override
    public List<GaenKey> filter(long now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) {
        return content.stream().filter(key -> key.getRollingPeriod() >= 0).collect(Collectors.toList());
    }
    
}