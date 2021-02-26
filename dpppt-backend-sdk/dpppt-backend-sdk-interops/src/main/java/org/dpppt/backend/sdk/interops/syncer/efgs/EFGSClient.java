package org.dpppt.backend.sdk.interops.syncer.efgs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.dpppt.backend.sdk.interops.utils.RestTemplateHelper;
import org.dpppt.backend.sdk.model.gaen.GaenKeyWithOrigin;
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

  public List<GaenKeyWithOrigin> download(LocalDate keyDate, String lastBatchTag) {
    // TODO
    return new ArrayList<>();
  }

  public void upload(List<GaenKeyWithOrigin> keysToUpload, String batchTag) {
    // TODO
  }
}
