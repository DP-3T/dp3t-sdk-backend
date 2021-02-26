package org.dpppt.backend.sdk.interops.model;

public class EfgsGatewayConfig {
  private String id;
  private String baseUrl;
  private String clientCert;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getClientCert() {
    return clientCert;
  }

  public void setClientCert(String clientCert) {
    this.clientCert = clientCert;
  }
}
