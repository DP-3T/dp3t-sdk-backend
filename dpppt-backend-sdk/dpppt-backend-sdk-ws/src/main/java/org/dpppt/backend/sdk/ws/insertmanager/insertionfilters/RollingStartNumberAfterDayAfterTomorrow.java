package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

public class RollingStartNumberAfterDayAfterTomorrow implements InsertionFilter{

    @Override
    public List<GaenKey> filter(UTCInstant now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) {
        return content.stream().filter(key -> {
            if(key.getRollingStartNumber() >= now.plusDays(2).atStartOfDay().get10MinutesSince1970()){
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }
    
}