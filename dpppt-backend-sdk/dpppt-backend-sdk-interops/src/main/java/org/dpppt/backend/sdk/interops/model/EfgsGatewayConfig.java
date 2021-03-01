package org.dpppt.backend.sdk.interops.model;

import java.util.List;

public class EfgsGatewayConfig {
  private String id;
  private String baseUrl;
  private String authClientCert; // P12 base-64 encoded
  private String authClientCertPassword; // string secret
  private String signClientCert; // PEM
  private String signClientCertPrivateKey; // PEM
  private String signAlgorithmName;
  private List<String> visitedCountries;

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

  public String getAuthClientCert() {
    return authClientCert;
  }

  public void setAuthClientCert(String authClientCert) {
    this.authClientCert = authClientCert;
  }

  public String getAuthClientCertPassword() {
    return authClientCertPassword;
  }

  public void setAuthClientCertPassword(String authClientCertPassword) {
    this.authClientCertPassword = authClientCertPassword;
  }

  public String getSignClientCert() {
    return signClientCert;
  }

  public void setSignClientCert(String signClientCert) {
    this.signClientCert = signClientCert;
  }

  public String getSignClientCertPrivateKey() {
    return signClientCertPrivateKey;
  }

  public void setSignClientCertPrivateKey(String signClientCertPrivateKey) {
    this.signClientCertPrivateKey = signClientCertPrivateKey;
  }

  public String getSignAlgorithmName() {
    return signAlgorithmName;
  }

  public void setSignAlgorithmName(String signAlgorithmName) {
    this.signAlgorithmName = signAlgorithmName;
  }

  public List<String> getVisitedCountries() {
    return visitedCountries;
  }

  public void setVisitedCountries(List<String> visitedCountries) {
    this.visitedCountries = visitedCountries;
  }
}
