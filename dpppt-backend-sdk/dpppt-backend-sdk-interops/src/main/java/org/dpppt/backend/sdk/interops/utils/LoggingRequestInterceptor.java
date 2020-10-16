package org.dpppt.backend.sdk.interops.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingRequestInterceptor.class);

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    traceRequest(request, body);
    ClientHttpResponse response = execution.execute(request, body);
    return traceResponse(response);
  }

  private void traceRequest(HttpRequest request, byte[] body) throws IOException {
    if (!LOGGER.isDebugEnabled()) {
      return;
    }
    LOGGER.debug(
        "==========================request begin==============================================");
    LOGGER.debug(request.getMethod() + " " + request.getURI());
    LOGGER.debug("Headers: {}", request.getHeaders());
    LOGGER.debug("Body:    {}", new String(body, "UTF-8"));
    LOGGER.debug(
        "==========================request end================================================");
  }

  private ClientHttpResponse traceResponse(ClientHttpResponse response) throws IOException {
    if (!LOGGER.isDebugEnabled()) {
      return response;
    }
    final ClientHttpResponse responseWrapper = new BufferingClientHttpResponseWrapper(response);
    StringBuilder inputStringBuilder = new StringBuilder();
    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(responseWrapper.getBody(), "UTF-8"));
    String line = bufferedReader.readLine();
    while (line != null) {
      inputStringBuilder.append(line);
      inputStringBuilder.append('\n');
      line = bufferedReader.readLine();
    }
    LOGGER.debug(
        "==========================response begin=============================================");
    LOGGER.debug("Status:  {}", responseWrapper.getStatusCode());
    LOGGER.debug("Headers: {}", responseWrapper.getHeaders());
    LOGGER.debug("Body:    {}", inputStringBuilder.toString());
    LOGGER.debug(
        "==========================response end===============================================");
    return responseWrapper;
  }
}
