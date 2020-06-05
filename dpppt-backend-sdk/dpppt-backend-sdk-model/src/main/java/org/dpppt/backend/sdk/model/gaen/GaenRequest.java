package org.dpppt.backend.sdk.model.gaen;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class GaenRequest {
    @NotNull
    @NotEmpty
    @Valid
    @Size(min = 14, max = 14)
    List<GaenKey> gaenKeys;

    @NotNull
    Integer delayedKeyDate;

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