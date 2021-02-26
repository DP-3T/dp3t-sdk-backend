package org.dpppt.backend.sdk.interops.syncer.efgs;

import org.dpppt.backend.sdk.interops.utils.RestTemplateHelper;
import org.springframework.web.client.RestTemplate;

public class EFGSClient {

  private final String baseUrl;
  private final RestTemplate rtDownload;
  private final RestTemplate rtUpload;

  public EFGSClient(
      String baseUrl,
      String authClientCert,
      String authClientCertPassword,
      String signClientCert,
      String signClientCertPassword) {

    this.baseUrl = baseUrl;

    this.rtDownload =
        RestTemplateHelper.getRestTemplateWithClientCerts(authClientCert, authClientCertPassword);
    this.rtUpload =
        RestTemplateHelper.getRestTemplateWithClientCerts(signClientCert, signClientCertPassword);
  }

  public String getBaseUrl() {
    return baseUrl;
  }
}
