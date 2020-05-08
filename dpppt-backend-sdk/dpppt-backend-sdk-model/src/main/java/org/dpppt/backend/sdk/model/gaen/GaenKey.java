package org.dpppt.backend.sdk.model.gaen;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class GaenKey {
    @NotNull
    @Size(min = 24, max = 24)
    String keyData;

    @NotNull
    Long rollingStartNumber;

    Long rollingPeriod = 144L;

    Integer transmissionRiskLevel = 0;


    public GaenKey() {
    }

    public GaenKey(String keyData, Long rollingStartNumber, Long rollingPeriod, Integer transmissionRiskLevel) {
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

    public Long getRollingStartNumber() {
        return this.rollingStartNumber;
    }

    public void setRollingStartNumber(Long rollingStartNumber) {
        this.rollingStartNumber = rollingStartNumber;
    }

    public Long getRollingPeriod() {
        return this.rollingPeriod;
    }

    public void setRollingPeriod(Long rollingPeriod) {
        this.rollingPeriod = rollingPeriod;
    }

    public Integer getTransmissionRiskLevel() {
        return this.transmissionRiskLevel;
    }

    public void setTransmissionRiskLevel(Integer transmissionRiskLevel) {
        this.transmissionRiskLevel = transmissionRiskLevel;
    }

}