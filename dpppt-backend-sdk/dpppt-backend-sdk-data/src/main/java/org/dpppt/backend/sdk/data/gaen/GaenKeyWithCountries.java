package org.dpppt.backend.sdk.data.gaen;

import ch.ubique.openapi.docannotations.Documentation;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.dpppt.backend.sdk.model.gaen.GaenKey;

/**
 * Same as {@link GaenKey} but adds the additional information for international interoperartions,
 * this is the origin country and the list of countries the key is user for.
 */
public class GaenKeyWithCountries {

  private GaenKey gaenKey;

  @NotNull
  @Documentation(description = "the list of countries the key is used for.")
  private List<String> countries = new ArrayList<>();

  @NotNull
  @Documentation(description = "the country of origin")
  private String origin;

  public GaenKey getGaenKey() {
    return gaenKey;
  }

  public void setGaenKey(GaenKey gaenKey) {
    this.gaenKey = gaenKey;
  }

  public List<String> getCountries() {
    return countries;
  }

  public void setCountries(List<String> countries) {
    this.countries = countries;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
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
