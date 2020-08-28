/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.dpppt.backend.sdk.model.ExposedKey;
import org.dpppt.backend.sdk.model.ExposeeAuthData;
import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.ExposeeRequestList;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

@SpringBootTest(
    properties = {
      "ws.app.jwt.publickey=classpath://generated_pub.pem",
      "ws.gaen.randomkeysenabled=true"
    })
public class DPPPTControllerTest extends BaseControllerTest {
  @Test
  public void testHello() throws Exception {
    MockHttpServletResponse response =
        mockMvc.perform(get("/v1")).andExpect(status().is2xxSuccessful()).andReturn().getResponse();

    assertNotNull(response);
    assertEquals("Hello from DP3T WS", response.getContentAsString());
  }

  @Test
  public void testJWT() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(utcNow.getTimestamp());
    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    exposeeRequest.setIsFake(0);
    String token = createToken(utcNow.plusMinutes(5));
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void testMultipleKeyUpload() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    var requestList = new ExposeeRequestList();
    var exposedKey1 = new ExposedKey();
    exposedKey1.setKeyDate(utcNow.getTimestamp());
    exposedKey1.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    var exposedKey2 = new ExposedKey();
    exposedKey2.setKeyDate(utcNow.minusDays(1).getTimestamp());
    exposedKey2.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    List<ExposedKey> exposedKeys = List.of(exposedKey1, exposedKey2);
    requestList.setExposedKeys(exposedKeys);
    requestList.setFake(0);
    String token = createToken(utcNow.plusMinutes(5));
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(requestList)))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(
                post("/v1/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "MockMVC")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void testMultipleKeyFakeUpload() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    var requestList = new ExposeeRequestList();
    var exposedKey1 = new ExposedKey();
    exposedKey1.setKeyDate(utcNow.getTimestamp());
    exposedKey1.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    var exposedKey2 = new ExposedKey();
    exposedKey2.setKeyDate(utcNow.minusDays(1).getTimestamp());
    exposedKey2.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    List<ExposedKey> exposedKeys = List.of(exposedKey1, exposedKey2);
    requestList.setExposedKeys(exposedKeys);
    requestList.setFake(1);
    String token = createToken(utcNow.plusMinutes(5));
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(requestList)))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(
                post("/v1/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "MockMVC")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void testMultipleKeyNonEmptyUpload() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    var requestList = new ExposeeRequestList();
    List<ExposedKey> exposedKeys = new ArrayList<ExposedKey>();
    requestList.setExposedKeys(exposedKeys);
    requestList.setFake(0);
    String token = createToken(utcNow.plusMinutes(5));
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(requestList)))
            .andExpect(status().is(400))
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(
                post("/v1/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "MockMVC")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void testMultipleKeyNonNullUpload() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    var requestList = new ExposeeRequestList();
    List<ExposedKey> exposedKeys = new ArrayList<ExposedKey>();
    requestList.setFake(0);
    String token = createToken(utcNow.plusMinutes(5));
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(requestList)))
            .andExpect(status().is(400))
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(
                post("/v1/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "MockMVC")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void keyNeedsToBeBase64() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(utcNow.getTimestamp());
    exposeeRequest.setKey("testKey32Bytes--testKey32Bytes--");
    exposeeRequest.setIsFake(1);
    String token = createToken(true, utcNow.plusMinutes(5));
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is(400))
            .andReturn()
            .getResponse();
  }

  @Test
  public void testJWTFake() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(utcNow.getTimestamp());
    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    exposeeRequest.setIsFake(1);
    String token = createToken(true, utcNow.plusMinutes(5));
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void cannotUseKeyDateBeforeOnset() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(UTCInstant.today().minusDays(2).getTimestamp());
    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    exposeeRequest.setIsFake(1);
    String token =
        createToken(
            UTCInstant.now().plusMinutes(5),
            UTCInstant.now().getLocalDate().format(DateTimeFormatter.ISO_DATE));

    mockMvc
        .perform(
            post("/v1/exposed")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "MockMVC")
                .content(json(exposeeRequest)))
        .andExpect(status().is(400))
        .andReturn()
        .getResponse();
  }

  @Test
  public void cannotUseSameTokenTwice() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(utcNow.getTimestamp());
    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    exposeeRequest.setIsFake(0);
    String token = createToken(utcNow.plusMinutes(5));

    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void canUseSameTokenTwiceIfFake() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(utcNow.getTimestamp());
    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    exposeeRequest.setIsFake(1);
    String token = createToken(true, utcNow.plusMinutes(5));

    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();
  }

  @Test
  public void cannotUseExpiredToken() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(utcNow.getTimestamp());
    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    exposeeRequest.setIsFake(0);
    String token = createToken(utcNow.minusMinutes(5));

    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn()
            .getResponse();
  }

  @Test
  public void cannotUseKeyDateInFuture() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(UTCInstant.now().plusDays(2).getTimestamp());
    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));

    exposeeRequest.setIsFake(0);
    String token = createToken(UTCInstant.now().plusMinutes(5));

    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn()
            .getResponse();
  }

  @Test
  public void keyDateNotOlderThan21Days() throws Exception {
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(UTCInstant.now().minusDays(22).getTimestamp());

    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    exposeeRequest.setIsFake(0);
    String token = createToken(UTCInstant.now().plusMinutes(5), "2020-01-01");

    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is4xxClientError())
            .andReturn()
            .getResponse();
  }

  @Test
  public void cannotUseTokenWithWrongScope() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(utcNow.getTimestamp());
    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    exposeeRequest.setIsFake(0);
    String token = createTokenWithScope(utcNow.plusMinutes(5), "not-exposed");

    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is(403))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();

    // Also for a 403 response, the token cannot be used a 2nd time
    response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void cannotUsedLongLivedToken() throws Exception {
    var utcNow = UTCInstant.now();
    var midnight = UTCInstant.today();
    ExposeeRequest exposeeRequest = new ExposeeRequest();
    exposeeRequest.setAuthData(new ExposeeAuthData());
    exposeeRequest.setKeyDate(utcNow.getTimestamp());
    exposeeRequest.setKey(
        Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
    exposeeRequest.setIsFake(0);
    String token = createToken(utcNow.plusDays(90)); // very late expiration date
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }
}
