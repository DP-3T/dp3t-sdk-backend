package org.dpppt.backend.sdk.model.gaen;

import java.util.List;

public class DayBuckets {
    String day;
    List<String> bucketUrls;


    public String getDay() {
        return this.day;
    }

    public List<String> getRelativeUrls() {
        return this.bucketUrls;
    }

    public DayBuckets day(String day) {
        this.day = day;
        return this;
    }

    public DayBuckets bucketUrls(List<String> bucketUrls) {
        this.bucketUrls = bucketUrls;
        return this;
    }
    
}