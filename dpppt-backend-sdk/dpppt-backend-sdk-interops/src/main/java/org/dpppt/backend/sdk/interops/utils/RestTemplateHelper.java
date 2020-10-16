package org.dpppt.backend.sdk.interops.utils;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class RestTemplateHelper {

  private static final int CONNECT_TIMEOUT = 20000;
  private static final int SOCKET_TIMEOUT = 20000;

  public static RestTemplate getRestTemplate() {
    RestTemplate rt = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient()));
    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
    interceptors.add(new LoggingRequestInterceptor());
    rt.setInterceptors(interceptors);
    return rt;
  }

  private static CloseableHttpClient httpClient() {
    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
    manager.setDefaultMaxPerRoute(20);
    manager.setMaxTotal(30);

    // Create HttpClientBuilder using system properties including proxy settings
    HttpClientBuilder builder =
        HttpClients.custom().useSystemProperties().setUserAgent("dp3t-interops");
    builder
        .setConnectionManager(manager)
        .disableCookieManagement()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build());
    return builder.build();
  }
}
