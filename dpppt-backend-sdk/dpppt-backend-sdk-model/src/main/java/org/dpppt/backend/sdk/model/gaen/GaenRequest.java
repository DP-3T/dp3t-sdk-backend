package org.dpppt.backend.sdk.model.gaen;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class GaenRequest {
    @NotNull
    @NotEmpty
    @Valid
    List<GaenKey> gaenKeys;

    Integer fake = 0;


    public List<GaenKey> getGaenKeys() {
        return this.gaenKeys;
    }

    public void setGaenKeys(List<GaenKey> gaenKeys) {
        this.gaenKeys = gaenKeys;
    }

    public Integer isFake() {
        return this.fake;
    }

    public void setFake(Integer fake) {
        this.fake = fake;
    }

}