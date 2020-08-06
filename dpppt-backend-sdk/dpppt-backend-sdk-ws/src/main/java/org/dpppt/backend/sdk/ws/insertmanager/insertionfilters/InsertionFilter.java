package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.InsertException;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

public interface InsertionFilter {
    public List<GaenKey> filter(UTCInstant now, List<GaenKey> content, OSType osType, Version osVersion, Version appVersion, Object principal) throws InsertException;
}