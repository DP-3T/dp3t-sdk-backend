package org.dpppt.backend.sdk.model.gaen;

import ch.ubique.openapi.docannotations.Documentation;
import javax.validation.constraints.NotNull;
import org.dpppt.backend.sdk.utils.UTCInstant;

/**
 * Same as {@link GaenKey} but adds the additional information for international interoperations,
 * this is the origin country.
 */
public class GaenKeyForInterops {

  private GaenKey gaenKey;

  @NotNull
  @Documentation(description = "the country of origin")
  private String origin;

  @Documentation(description = "the report type of the key")
  private ReportType reportType;

  @Documentation(description = "day since onset of symptoms")
  private Integer daysSinceOnsetOfSymptoms;

  @NotNull
  @Documentation(description = "unique identifier (auto-generated from db)")
  private Integer id;

  @Documentation(description = "timestamp the key was received at the server")
  private UTCInstant receivedAt;

  public GaenKey getGaenKey() {
    return gaenKey;
  }

  public void setGaenKey(GaenKey gaenKey) {
    this.gaenKey = gaenKey;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public ReportType getReportType() {
    return reportType;
  }

  public void setReportType(ReportType reportType) {
    this.reportType = reportType;
  }

  public Integer getDaysSinceOnsetOfSymptoms() {
    return daysSinceOnsetOfSymptoms;
  }

  public void setDaysSinceOnsetOfSymptoms(Integer daysSinceOnsetOfSymptoms) {
    this.daysSinceOnsetOfSymptoms = daysSinceOnsetOfSymptoms;
  }

  public UTCInstant getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(UTCInstant receivedAt) {
    this.receivedAt = receivedAt;
  }

  public String getKeyData() {
    return gaenKey.getKeyData();
  }

  public void setKeyData(String keyData) {
    gaenKey.setKeyData(keyData);
  }

  public Integer getRollingStartNumber() {
    return gaenKey.getRollingStartNumber();
  }

  public void setRollingStartNumber(Integer rollingStartNumber) {
    gaenKey.setRollingStartNumber(rollingStartNumber);
  }

  public Integer getRollingPeriod() {
    return gaenKey.getRollingPeriod();
  }

  public void setRollingPeriod(Integer rollingPeriod) {
    gaenKey.setRollingPeriod(rollingPeriod);
  }

  public Integer getTransmissionRiskLevel() {
    return gaenKey.getTransmissionRiskLevel();
  }

  public void setTransmissionRiskLevel(Integer transmissionRiskLevel) {
    gaenKey.setTransmissionRiskLevel(transmissionRiskLevel);
  }

  public Integer getFake() {
    return gaenKey.getFake();
  }

  public void setFake(Integer fake) {
    gaenKey.setFake(fake);
  }
}
