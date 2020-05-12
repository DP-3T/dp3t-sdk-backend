package org.dpppt.backend.sdk.model.gaen;

public class Header {
    Long startTimestamp;
    Long endTimestamp;
    String region;
    Integer batchNum;
    Integer batchSize;


    public Long getStartTimestamp() {
        return this.startTimestamp;
    }

    public Long getEndTimestamp() {
        return this.endTimestamp;
    }

    public String getRegion() {
        return this.region;
    }

    public Integer getBatchNum() {
        return this.batchNum;
    }

    public Integer getBatchSize() {
        return this.batchSize;
    }

    public Header startTimestamp(Long startTimestamp) {
        this.startTimestamp = startTimestamp;
        return this;
    }

    public Header endTimestamp(Long endTimestamp) {
        this.endTimestamp = endTimestamp;
        return this;
    }

    public Header region(String region) {
        this.region = region;
        return this;
    }

    public Header batchNum(Integer batchNum) {
        this.batchNum = batchNum;
        return this;
    }

    public Header batchSize(Integer batchSize) {
        this.batchSize = batchSize;
        return this;
    }

}