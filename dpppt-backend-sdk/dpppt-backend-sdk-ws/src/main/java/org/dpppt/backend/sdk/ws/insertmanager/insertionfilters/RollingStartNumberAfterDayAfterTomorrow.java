package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

public class RollingStartNumberAfterDayAfterTomorrow implements InsertionFilter{

    @Override
    public List<GaenKey> filter(long now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion) {
        return content.stream().filter(key -> {
            if(key.getRollingStartNumber() > now + 2 * 144){
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }
    
}