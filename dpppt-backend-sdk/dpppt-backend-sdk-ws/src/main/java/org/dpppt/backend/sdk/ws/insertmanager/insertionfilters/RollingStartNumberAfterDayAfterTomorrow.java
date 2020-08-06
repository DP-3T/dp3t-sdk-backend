package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

public class RollingStartNumberAfterDayAfterTomorrow implements InsertionFilter{

    @Override
    public List<GaenKey> filter(UTCInstant now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) {
        return content.stream().filter(key -> {
            var rollingStartNumberInstant = UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
            return rollingStartNumberInstant.isBeforeDateOf(now.plusDays(2));
        }).collect(Collectors.toList());
    }
    
}