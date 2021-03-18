/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

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
import java.util.Base64;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
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
    return buildRestTemplate(null, null, null);
  }

  public static RestTemplate getRestTemplateWithClientCerts(
      String authClientCert, String authClientCertPassword, List<String> allowedHostnames) {
    return buildRestTemplate(authClientCert, authClientCertPassword, allowedHostnames);
  }

  private static RestTemplate buildRestTemplate(
      String authClientCert, String authClientCertPassword, List<String> allowedHostnames) {
    try {
      RestTemplate rt =
          new RestTemplate(
              new HttpComponentsClientHttpRequestFactory(
                  httpClient(authClientCert, authClientCertPassword, allowedHostnames)));
      List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
      interceptors.add(new LoggingRequestInterceptor());
      rt.setInterceptors(interceptors);
      return rt;
    } catch (Exception e) {
      throw new RuntimeException("Could not create resttemplate", e);
    }
  }

  private static CloseableHttpClient httpClient(
      String clientCert, String clientCertPassword, List<String> allowedHostnames)
      throws IOException, KeyManagementException, UnrecoverableKeyException,
          NoSuchAlgorithmException, KeyStoreException, CertificateException {
    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();

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
                  (aliases, socket) ->
                      !aliases.keySet().isEmpty() ? aliases.keySet().iterator().next() : null)
              .build();
      builder.setSSLContext(sslContext);

      SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
      Registry<ConnectionSocketFactory> socketFactoryRegistry =
          RegistryBuilder.<ConnectionSocketFactory>create()
              .register("https", sslsf)
              .register("http", PlainConnectionSocketFactory.INSTANCE)
              .build();
      manager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
    }
    manager.setDefaultMaxPerRoute(20);
    manager.setMaxTotal(30);

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
    final String base64Protocol = "base64:/";
    if (path.startsWith(base64Protocol)) {
      byte[] decodedBytes = Base64.getDecoder().decode(path.replace(base64Protocol, ""));
      file = Files.createTempFile(null, null);
      Files.write(file.toAbsolutePath(), decodedBytes);
    } else if (path.startsWith("classpath:/")) {
      InputStream in = createInputStream(path);
      file = Files.createTempFile(null, null);
      Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
      in.close();
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
