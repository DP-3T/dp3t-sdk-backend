package org.dpppt.backend.sdk.model.gaen;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class GaenKey {
    @NotNull
    @Size(min = 24, max = 24)
    String keyData;

    @NotNull
    Integer rollingStartNumber;

    @NotNull
    Integer rollingPeriod;

    @NotNull
    Integer transmissionRiskLevel;

    Integer fake = 0;



    public GaenKey() {
    }

    public GaenKey(String keyData, Integer rollingStartNumber, Integer rollingPeriod, Integer transmissionRiskLevel) {
        this.keyData = keyData;
        this.rollingStartNumber = rollingStartNumber;
        this.rollingPeriod = rollingPeriod;
        this.transmissionRiskLevel = transmissionRiskLevel;
    }

    public String getKeyData() {
        return this.keyData;
    }

    public void setKeyData(String keyData) {
        this.keyData = keyData;
    }

    public Integer getRollingStartNumber() {
        return this.rollingStartNumber;
    }

    public void setRollingStartNumber(Integer rollingStartNumber) {
        this.rollingStartNumber = rollingStartNumber;
    }

    public Integer getRollingPeriod() {
        return this.rollingPeriod;
    }

    public void setRollingPeriod(Integer rollingPeriod) {
        this.rollingPeriod = rollingPeriod;
    }

    public Integer getTransmissionRiskLevel() {
        return this.transmissionRiskLevel;
    }

    public void setTransmissionRiskLevel(Integer transmissionRiskLevel) {
        this.transmissionRiskLevel = transmissionRiskLevel;
    }

    public Integer getFake() {
        return this.fake;
    }

    public void setFake(Integer fake) {
        this.fake = fake;
    }

}