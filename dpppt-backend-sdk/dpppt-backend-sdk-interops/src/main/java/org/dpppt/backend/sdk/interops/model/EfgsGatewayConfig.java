/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.interops.model;

import java.util.List;
import org.dpppt.backend.sdk.model.gaen.ReportType;

public class EfgsGatewayConfig {
  private String id;
  private String baseUrl;
  private String authClientCert; // P12 base-64 encoded
  private String authClientCertPassword; // string secret
  private String signClientCert; // P12 base-64 encoded
  private String signClientCertPassword; // string secret
  private String signAlgorithmName;
  private List<String> visitedCountries;
  private Integer defaultTransmissionRiskLevel = Integer.MAX_VALUE;
  private ReportType defaultReportType = ReportType.CONFIRMED_TEST;

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

  public String getSignClientCertPassword() {
    return signClientCertPassword;
  }

  public void setSignClientCertPassword(String signClientCertPassword) {
    this.signClientCertPassword = signClientCertPassword;
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

  public Integer getDefaultTransmissionRiskLevel() {
    return defaultTransmissionRiskLevel;
  }

  public void setDefaultTransmissionRiskLevel(Integer defaultTransmissionRiskLevel) {
    this.defaultTransmissionRiskLevel = defaultTransmissionRiskLevel;
  }

  public ReportType getDefaultReportType() {
    return defaultReportType;
  }

  public void setDefaultReportType(ReportType defaultReportType) {
    this.defaultReportType = defaultReportType;
  }
}
