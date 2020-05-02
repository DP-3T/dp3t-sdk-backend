package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;
import java.util.Base64;

public class InfectedSignature {

    @NotNull
    private String base64signature;

    public InfectedSignature() {}

    public InfectedSignature(byte[] signature) {
        assert signature != null && signature.length > 0;
        this.base64signature = Base64.getEncoder().encodeToString(signature);
    }

    public String getBase64signature() {
        return base64signature;
    }
}
