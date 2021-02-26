package org.dpppt.backend.sdk.model.gaen;

import ch.ubique.openapi.docannotations.Documentation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class GaenV2UploadKeysRequest {

  @Valid
  @Documentation(description = "If true, the uploaded keys can be shared with federations gateway")
  private Boolean withFederationGateway;

  @NotNull
  @NotEmpty
  @Valid
  @Size(min = 30, max = 30)
  @Documentation(
      description = "30 Temporary Exposure Keys - zero or more of them might be fake keys.")
  private List<GaenKey> gaenKeys;

  public List<GaenKey> getGaenKeys() {
    return this.gaenKeys;
  }

  public void setGaenKeys(List<GaenKey> gaenKeys) {
    this.gaenKeys = gaenKeys;
  }

  public Boolean getWithFederationGateway() {
    return withFederationGateway;
  }

  public void setWithFederationGateway(Boolean withFederationGateway) {
    this.withFederationGateway = withFederationGateway;
  }
}
