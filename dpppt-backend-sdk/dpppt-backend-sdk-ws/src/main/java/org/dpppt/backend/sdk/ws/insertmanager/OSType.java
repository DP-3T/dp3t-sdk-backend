package org.dpppt.backend.sdk.ws.insertmanager;

public enum OSType {
    ANDROID,
    IOS;

    @Override
    public String toString() {
        switch(this) {
            case ANDROID: return "Android";
            case IOS: return "iOS";
            default: return "Unknown";
        }
    }
}