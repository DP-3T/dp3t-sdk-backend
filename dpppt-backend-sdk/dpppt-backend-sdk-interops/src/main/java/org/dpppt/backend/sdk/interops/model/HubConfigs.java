package org.dpppt.backend.sdk.interops.model;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "interops.hubs")
public class HubConfigs {
  private List<EfgsGatewayConfig> efgsGateways;

  public List<EfgsGatewayConfig> getEfgsGateways() {
    return efgsGateways;
  }

  public void setEfgsGateways(List<EfgsGatewayConfig> efgsGateways) {
    this.efgsGateways = efgsGateways;
  }
}
