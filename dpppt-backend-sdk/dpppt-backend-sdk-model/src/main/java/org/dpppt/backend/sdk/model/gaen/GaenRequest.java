package org.dpppt.backend.sdk.model.gaen;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class GaenRequest {
    @NotNull
    @NotEmpty
    List<GaenKey> gaenKeys;


    public List<GaenKey> getGaenKeys() {
        return this.gaenKeys;
    }

    public void setGaenKeys(List<GaenKey> gaenKeys) {
        this.gaenKeys = gaenKeys;
    }
}