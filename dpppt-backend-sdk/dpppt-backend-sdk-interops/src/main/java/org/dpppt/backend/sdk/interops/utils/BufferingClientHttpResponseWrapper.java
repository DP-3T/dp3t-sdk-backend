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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

public final class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

  private final ClientHttpResponse response;

  private byte[] body;

  BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
    this.response = response;
  }

  public HttpStatus getStatusCode() throws IOException {
    return this.response.getStatusCode();
  }

  public int getRawStatusCode() throws IOException {
    return this.response.getRawStatusCode();
  }

  public String getStatusText() throws IOException {
    return this.response.getStatusText();
  }

  public HttpHeaders getHeaders() {
    return this.response.getHeaders();
  }

  public InputStream getBody() throws IOException {
    if (this.body == null) {
      this.body = StreamUtils.copyToByteArray(this.response.getBody());
    }
    return new ByteArrayInputStream(this.body);
  }

  public void close() {
    this.response.close();
  }
}
