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
    public void insertIntoDatabase(List<GaenKey> keys, String header, Object principal){
        if(keys.isEmpty()) {
            return;
        }
        var now = System.currentTimeMillis();
        var internalKeys = keys;
        var headerParts = header.split(";");
        if(headerParts.length != 5) {
            return;
        } 
        var osType = exctractOS(headerParts[3]);
        var osVersion = extractOsVersion(headerParts[4]);
        var appVersion = extractAppVersion(headerParts[1], headerParts[2]);
        for(InsertionFilter filter : filterList){
            internalKeys = filter.filter(now, internalKeys, osType, osVersion, appVersion, principal);
        }
        if(internalKeys.isEmpty()) {
            return;
        }
        dataService.upsertExposees(internalKeys);
    }
    //ch.admin.bag.dp36;1.0.7,200724.1105.215;iOS;13.6
    //ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29
    private OSType exctractOS(String osString) {
        var result = OSType.ANDROID;
        switch(osString.toLowerCase()) {
            case "ios": 
                result = OSType.IOS;
                break;
            case "android":
            break;
            default:
                result = OSType.ANDROID;
        }
        return result;
    }
    
    private Version extractOsVersion(String osVersionString) {
        return new Version(osVersionString);
    }
    private Version extractAppVersion(String osAppVersionString, String osMetaInfo){
        return new Version(osAppVersionString+"+"+osMetaInfo);
    }
}