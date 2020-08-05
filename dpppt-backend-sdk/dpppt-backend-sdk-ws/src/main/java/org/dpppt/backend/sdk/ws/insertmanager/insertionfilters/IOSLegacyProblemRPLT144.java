package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

// This feature makes sure rolling period is always set to 144
// default value according to EN is 144, so just set it to that.				
// This allows to check for the Google-TEKs also on iOS. Because the Rolling Proximity Identifier
// is based on the TEK and the unix epoch, this should work. The only downside is that iOS will
// not be able to optimize verification of the TEKs, because it will have to consider each TEK for
// a whole day.
public class IOSLegacyProblemRPLT144 implements InsertionFilter {

    
    @Override
    public List<GaenKey> filter(UTCInstant now,List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) {
        for(GaenKey key : content){
            key.setRollingPeriod(144);
        }
        return content;
    }
}