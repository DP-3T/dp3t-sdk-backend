package org.dpppt.backend.sdk.interops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IrishHubDownloadResponse {

  private String batchTag;
  private List<IrishHubKey> exposures;

  public String getBatchTag() {
    return batchTag;
  }

  public void setBatchTag(String batchTag) {
    this.batchTag = batchTag;
  }

  public List<IrishHubKey> getExposures() {
    return exposures;
  }

  public void setExposures(List<IrishHubKey> exposures) {
    this.exposures = exposures;
  }
}
