package org.dpppt.backend.sdk.model.gaen;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class GaenRequest {
    //TODO: needs to be adjusted properly after upgrading. (should ensure that payload stays sonstant)
    @NotNull
    @NotEmpty
    @Valid
    @Size(min = 14, max = 30)
    private List<GaenKey> gaenKeys;

    @NotNull
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