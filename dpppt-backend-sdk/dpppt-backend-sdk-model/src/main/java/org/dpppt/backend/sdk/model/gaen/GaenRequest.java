package org.dpppt.backend.sdk.model.gaen;

import ch.ubique.openapi.docannotations.Documentation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * GaenRequest represents a request made by the client to the backend-server. It is used to publish
 * the _Exposed Keys_ to the backend server. As the GAEN API on the mobile phones never release the
 * current day's key, the client can set _delayedKeyDate_ to indicate a delayed key.
 */
public class GaenRequest {
  @NotNull
  @NotEmpty
  @Valid
  @Size(min = 14, max = 30)
  @Documentation(
      description =
          "Between 14 and 30 Temporary Exposure Keys - zero or more of them might be fake keys."
              + " Starting with EN 1.5 it is possible that clients send more than 14 keys.")
  private List<GaenKey> gaenKeys;

  @NotNull
  @Documentation(
      description =
          "Prior to version 1.5 Exposure Keys for the day of report weren't available (since they"
              + " were still used throughout this day RPI=144), so the submission of the last key"
              + " had to be delayed. This Unix timestamp in milliseconds specifies, which key date"
              + " the last key (which will be submitted on the next day) will have. The backend"
              + " then issues a JWT to allow the submission of this last key with specified key"
              + " date. This should not be necessary after the Exposure Framework is able to send"
              + " and handle keys with RollingPeriod < 144 (e.g. only valid until submission).")
  private Integer delayedKeyDate;

  public List<GaenKey> getGaenKeys() {
    return this.gaenKeys;
  }

  public void setGaenKeys(List<GaenKey> gaenKeys) {
    this.gaenKeys = gaenKeys;
  }

  public Integer getDelayedKeyDate() {
    return this.delayedKeyDate;
  }

  public void setDelayedKeyDate(Integer delayedKeyDate) {
    this.delayedKeyDate = delayedKeyDate;
  }
}
