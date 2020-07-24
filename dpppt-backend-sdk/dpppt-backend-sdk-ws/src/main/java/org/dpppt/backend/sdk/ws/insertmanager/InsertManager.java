package org.dpppt.backend.sdk.ws.insertmanager;

import java.util.ArrayList;
import java.util.List;

import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.ws.insertmanager.insertionfilters.InsertionFilter;

public class InsertManager {
    private ArrayList<InsertionFilter> filterList = new ArrayList<>();
    private final GAENDataService dataService;
    public InsertManager(GAENDataService dataService){
        this.dataService = dataService;
    }
    public void addFilter(InsertionFilter filter) {
        filterList.add(filter);
    }
    public void insertIntoDatabase(List<GaenKey> keys, OSType osType, Version osVersion, Version appVersion){
        var now = System.currentTimeMillis();
        var internalKeys = keys;
        for(InsertionFilter filter : filterList){
            internalKeys = filter.filter(now, keys, osType, osVersion, appVersion);
        }
        dataService.upsertExposees(internalKeys);
    }
}