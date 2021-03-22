package org.dpppt.backend.sdk.model.interops;

import java.time.LocalDate;
import org.dpppt.backend.sdk.utils.UTCInstant;

public class FederationSyncLogEntry {
  private Integer id;
  private String gateway;
  private SyncAction action;
  private String batchTag;
  private LocalDate uploadDate;
  private UTCInstant startTime;
  private UTCInstant endTime;
  private SyncState state;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getGateway() {
    return gateway;
  }

  public void setGateway(String gateway) {
    this.gateway = gateway;
  }

  public SyncAction getAction() {
    return action;
  }

  public void setAction(SyncAction action) {
    this.action = action;
  }

  public String getBatchTag() {
    return batchTag;
  }

  public void setBatchTag(String batchTag) {
    this.batchTag = batchTag;
  }

  public LocalDate getUploadDate() {
    return uploadDate;
  }

  public void setUploadDate(LocalDate uploadDate) {
    this.uploadDate = uploadDate;
  }

  public UTCInstant getStartTime() {
    return startTime;
  }

  public void setStartTime(UTCInstant startTime) {
    this.startTime = startTime;
  }

  public UTCInstant getEndTime() {
    return endTime;
  }

  public void setEndTime(UTCInstant endTime) {
    this.endTime = endTime;
  }

  public SyncState getState() {
    return state;
  }

  public void setState(SyncState state) {
    this.state = state;
  }
}
