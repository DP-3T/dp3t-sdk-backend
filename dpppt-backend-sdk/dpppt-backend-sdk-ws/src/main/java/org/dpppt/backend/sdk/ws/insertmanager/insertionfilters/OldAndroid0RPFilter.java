package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;

import org.dpppt.backend.sdk.model.gaen.GaenKey;

import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Currently only android seems to send 0 which can never be valid, since a non used key should not be submitted
// default value according to EN is 144, so just set it to that.
// If we ever get 0 from iOS we should log it, since this should not happen
public class OldAndroid0RPFilter implements InsertionFilter {
    private static final Logger logger = LoggerFactory.getLogger(OldAndroid0RPFilter.class);
    
    @Override
    public List<GaenKey> filter(UTCInstant now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) {
        for(Object key : content){
            if(key instanceof GaenKey){
                var gaenKey = (GaenKey)key;
                if(gaenKey.getRollingPeriod().equals(0))
                    if(osType.equals(OSType.IOS)) {
                        logger.error("We got a rollingPeriod of 0 ({},{},{})", osType, osVersion, appVersion);
                    }
                    gaenKey.setRollingPeriod(144);
            }
        }
        return content;
    }

}