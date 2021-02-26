package org.dpppt.backend.sdk.interops.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class RestTemplateHelper {

  private static final String DP3T_INTEROPS = "dp3t-interops";
  private static final int CONNECT_TIMEOUT = 20000;
  private static final int SOCKET_TIMEOUT = 20000;

  public static RestTemplate getRestTemplate() {
    return buildRestTemplate(null, null);
  }

  public static RestTemplate getRestTemplateWithClientCerts(
      String authClientCert, String authClientCertPassword) {
    return buildRestTemplate(authClientCert, authClientCertPassword);
  }

  private static RestTemplate buildRestTemplate(
      String authClientCert, String authClientCertPassword) {
    try {
      RestTemplate rt =
          new RestTemplate(
              new HttpComponentsClientHttpRequestFactory(
                  httpClient(authClientCert, authClientCertPassword)));
      List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
      interceptors.add(new LoggingRequestInterceptor());
      rt.setInterceptors(interceptors);
      return rt;
    } catch (Exception e) {
      throw new RuntimeException("Could not create resttemplate", e);
    }
  }

  private static CloseableHttpClient httpClient(String clientCert, String clientCertPassword)
      throws IOException, KeyManagementException, UnrecoverableKeyException,
          NoSuchAlgorithmException, KeyStoreException, CertificateException {
    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
    manager.setDefaultMaxPerRoute(20);
    manager.setMaxTotal(30);

    HttpClientBuilder builder = HttpClients.custom();
    builder
        .useSystemProperties()
        .setUserAgent(DP3T_INTEROPS)
        .setConnectionManager(manager)
        .disableCookieManagement()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build());

    if (clientCert != null && clientCertPassword != null) {
      Path clientCertFile = getFile(clientCert);
      SSLContext sslContext =
          SSLContexts.custom()
              .loadKeyMaterial(
                  clientCertFile.toFile(),
                  clientCertPassword.toCharArray(),
                  clientCertPassword.toCharArray(),
                  (aliases, socket) -> aliases.keySet().iterator().next())
              .build();
      builder.setSSLContext(sslContext);
    }

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

  public static Path getFile(String path) throws IOException {
    Path file = null;
    if (path.startsWith("classpath:/")) {
      InputStream in = createInputStream(path);
      file = Files.createTempFile(null, null);
      Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
      in.close();
      return file;
    } else {
      file = Paths.get(path);
    }
    return file;
  }

  public static InputStream createInputStream(String path) throws IOException {
    InputStream in = null;
    if (path.startsWith("classpath:/")) {
      in = classPathInputStream(path.substring(11));
    } else {
      in = new FileInputStream(path);
    }
    return in;
  }

  private static InputStream classPathInputStream(String src) throws IOException {
    return new ClassPathResource(src).getInputStream();
  }
}
