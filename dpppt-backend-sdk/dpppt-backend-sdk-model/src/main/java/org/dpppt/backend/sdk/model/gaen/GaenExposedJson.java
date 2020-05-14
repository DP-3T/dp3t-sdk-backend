package org.dpppt.backend.sdk.model.gaen;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class GaenExposedJson {
    @NotNull
    @NotEmpty
    List<GaenKey> gaenKeys;
    Header header;


    public List<GaenKey> getGaenKeys() {
        return this.gaenKeys;
    }

    public Header getHeader() {
        return this.header;
    }


    public GaenExposedJson gaenKeys(List<GaenKey> gaenKeys) {
        this.gaenKeys = gaenKeys;
        return this;
    }

    public GaenExposedJson header(Header header) {
        this.header = header;
        return this;
    }

}