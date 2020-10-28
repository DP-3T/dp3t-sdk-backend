package org.dpppt.backend.sdk.model.gaen;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class GaenSecondDay {
  @NotNull @Valid private GaenKey delayedKey;

  public GaenKey getDelayedKey() {
    return this.delayedKey;
  }

  public void setDelayedKey(GaenKey delayedKey) {
    this.delayedKey = delayedKey;
  }
}
