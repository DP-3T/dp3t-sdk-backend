package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

public class IOSLegacyProblemRPLT144 implements InsertionFilter {

    @Override
    public List<GaenKey> filter(long now,List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) {
        for(GaenKey key : content){
            key.setRollingPeriod(144);
        }
        return content;
    }
}