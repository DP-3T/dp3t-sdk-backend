package org.dpppt.backend.sdk.model;

import java.util.List;

public class ExposeeRequestList {
    List<ExposedKey> exposedKeys;

    private Integer fake = 0;

    public List<ExposedKey> getExposedKeys() {
        return exposedKeys;
    }

    public Integer isFake() {
        return fake;
    }

    public void setFake(Integer fake) {
        this.fake = fake;
    }

    public void setExposedKeys(List<ExposedKey> exposedKeys) {
        this.exposedKeys = exposedKeys;
    }
}