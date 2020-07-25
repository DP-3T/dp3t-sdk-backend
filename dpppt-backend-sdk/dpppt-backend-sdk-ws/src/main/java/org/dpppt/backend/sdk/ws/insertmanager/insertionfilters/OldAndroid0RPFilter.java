package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;

import org.dpppt.backend.sdk.model.gaen.GaenKey;

import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

public class OldAndroid0RPFilter implements InsertionFilter {

    @Override
    public List<GaenKey> filter(UTCInstant now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) {
        for(Object key : content){
            if(key instanceof GaenKey){
                var gaenKey = (GaenKey)key;
                if(gaenKey.getRollingPeriod().equals(0)
                && osType.equals(OSType.ANDROID))
                gaenKey.setRollingPeriod(144);
            }
        }
        return content;
    }

}