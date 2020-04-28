package org.dpppt.backend.sdk.model;

import java.util.ArrayList;
import java.util.List;

public class BucketList {
    List<Long> buckets;
    public List<Long> getBuckets() {
        return buckets;
    }
    public void setBuckets(List<Long> buckets) {
        this.buckets = buckets;
    }
    public void addBucket(Long bucket) {
        if(this.buckets == null) {
            this.buckets = new ArrayList<Long>();
        }
        this.buckets.add(bucket);
    }
}