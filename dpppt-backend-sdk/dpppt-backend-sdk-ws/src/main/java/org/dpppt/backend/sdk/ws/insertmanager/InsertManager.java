package org.dpppt.backend.sdk.ws.insertmanager;

import java.util.ArrayList;
import java.util.List;

import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.insertionfilters.InsertionFilter;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsertManager {
    private static final Logger logger = LoggerFactory.getLogger(InsertManager.class);

    private ArrayList<InsertionFilter> filterList = new ArrayList<>();
    private final GAENDataService dataService;
    private final ValidationUtils validationUtils;

    public InsertManager(GAENDataService dataService, ValidationUtils validationUtils){
        this.dataService = dataService;
        this.validationUtils = validationUtils;
    }
    public void addFilter(InsertionFilter filter) {
        filterList.add(filter);
    }
    public void insertIntoDatabase(List<GaenKey> keys, String header, Object principal, UTCInstant now) throws InsertException{
        if(keys == null || keys.isEmpty()) {
            return;
        }
        var internalKeys = keys;
        var headerParts = header.split(";");
        if(headerParts.length != 5) {
            headerParts = List.of("org.example.dp3t", "1.0.0", "0", "Android", "29").toArray(new String[0]);
            logger.error("We received a invalid header: {}", header.replaceAll("[\n|\r|\t]", "_"));
        } 
        var osType = exctractOS(headerParts[3]);
        var osVersion = extractOsVersion(headerParts[4]);
        var appVersion = extractAppVersion(headerParts[1], headerParts[2]);
        for(InsertionFilter filter : filterList){
            internalKeys = filter.filter(now, internalKeys, osType, osVersion, appVersion, principal);
        }
        if(internalKeys.isEmpty()
        || validationUtils.jwtIsFake(principal)) {
            return;
        }
        dataService.upsertExposees(internalKeys, now);
    }
    //ch.admin.bag.dp36;1.0.7;200724.1105.215;iOS;13.6
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