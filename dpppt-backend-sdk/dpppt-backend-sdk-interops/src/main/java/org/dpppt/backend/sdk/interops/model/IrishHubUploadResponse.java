package org.dpppt.backend.sdk.interops.model;

public class IrishHubUploadResponse {
  private String batchTag;
  private Integer insertedExposures;

  public String getBatchTag() {
    return batchTag;
  }

  public void setBatchTag(String batchTag) {
    this.batchTag = batchTag;
  }

  public Integer getInsertedExposures() {
    return insertedExposures;
  }

  public void setInsertedExposures(Integer insertedExposures) {
    this.insertedExposures = insertedExposures;
  }
}
