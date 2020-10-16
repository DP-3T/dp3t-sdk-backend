package org.dpppt.backend.sdk.model.gaen;

import ch.ubique.openapi.docannotations.Documentation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class GaenV2UploadKeysRequest {

  @Valid
  @Min(value = 0)
  @Max(value = 1)
  @Documentation(
      description =
          "If internationl = 0 key is only for the origin country. If international = 1 key is"
              + " distributed to other countries")
  private int international = 0;

  @NotNull
  @NotEmpty
  @Valid
  @Size(min = 30, max = 30)
  @Documentation(
      description = "30 Temporary Exposure Keys - zero or more of them might be fake keys.")
  private List<GaenKey> gaenKeys;

  public List<GaenKey> getGaenKeys() {
    return this.gaenKeys;
  }

  public void setGaenKeys(List<GaenKey> gaenKeys) {
    this.gaenKeys = gaenKeys;
  }

  public int getInternational() {
    return international;
  }

  public void setInternational(int international) {
    this.international = international;
  }
}
