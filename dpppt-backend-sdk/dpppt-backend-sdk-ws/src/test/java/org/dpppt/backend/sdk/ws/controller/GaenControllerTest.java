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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.model.gaen.GaenSecondDay;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat.TEKSignatureList;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat.TemporaryExposureKeyExport;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.security.KeyVault;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles({"actuator-security"})
@SpringBootTest(
    properties = {
      "ws.app.jwt.publickey=classpath://generated_pub.pem",
      "logging.level.org.springframework.security=DEBUG",
      "ws.exposedlist.releaseBucketDuration=7200000",
      "ws.gaen.randomkeysenabled=true",
      "ws.monitor.prometheus.user=prometheus",
      "ws.monitor.prometheus.password=prometheus",
      "management.endpoints.enabled-by-default=true",
      "management.endpoints.web.exposure.include=*"
    })
@Transactional
public class GaenControllerTest extends BaseControllerTest {
  @Autowired ProtoSignature signer;
  @Autowired KeyVault keyVault;
  @Autowired GAENDataService gaenDataService;
  Long releaseBucketDuration = 7200000L;

  private static final Logger logger = LoggerFactory.getLogger(GaenControllerTest.class);

  @Test
  public void testHello() throws Exception {
    MockHttpServletResponse response =
        mockMvc.perform(get("/v1")).andExpect(status().is2xxSuccessful()).andReturn().getResponse();

    assertNotNull(response);
    assertEquals("Hello from DP3T WS", response.getContentAsString());
  }

  @Test
  public void testActuatorSecurity() throws Exception {
    var response =
        mockMvc
            .perform(get("/actuator/health"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(get("/actuator/loggers"))
            .andExpect(status().is(401))
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(
                get("/actuator/loggers")
                    .header("Authorization", "Basic cHJvbWV0aGV1czpwcm9tZXRoZXVz"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(get("/actuator/prometheus"))
            .andExpect(status().is(401))
            .andReturn()
            .getResponse();
    response =
        mockMvc
            .perform(
                get("/actuator/prometheus")
                    .header("Authorization", "Basic cHJvbWV0aGV1czpwcm9tZXRoZXVz"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }

  private void testNKeys(UTCInstant now, int n, boolean shouldSucceed) throws Exception {
    var requestList = new GaenRequest();
    var gaenKey1 = new GaenKey();
    gaenKey1.setRollingStartNumber((int) now.atStartOfDay().minusDays(1).get10MinutesSince1970());
    gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes01".getBytes("UTF-8")));
    gaenKey1.setRollingPeriod(144);
    gaenKey1.setFake(0);
    gaenKey1.setTransmissionRiskLevel(0);
    var gaenKey2 = new GaenKey();
    gaenKey2.setRollingStartNumber((int) now.atStartOfDay().minusDays(1).get10MinutesSince1970());
    gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes02".getBytes("UTF-8")));
    gaenKey2.setRollingPeriod(144);
    gaenKey2.setFake(0);
    gaenKey2.setTransmissionRiskLevel(0);
    List<GaenKey> exposedKeys = new ArrayList<>();
    exposedKeys.add(gaenKey1);
    exposedKeys.add(gaenKey2);
    for (int i = 0; i < n - 2; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber((int) now.atStartOfDay().get10MinutesSince1970());
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      exposedKeys.add(tmpKey);
    }
    requestList.setGaenKeys(exposedKeys);
    var duration = now.atStartOfDay().plusDays(1).get10MinutesSince1970();
    requestList.setDelayedKeyDate((int) duration);
    gaenKey1.setFake(0);
    String token = createToken(now.plusMinutes(5));
    var requestBuilder =
        mockMvc.perform(
            post("/v1/gaen/exposed")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                .content(json(requestList)));
    MvcResult response;

    if (shouldSucceed) {
      response = requestBuilder.andExpect(request().asyncStarted()).andReturn();
      mockMvc.perform(asyncDispatch(response)).andExpect(status().is2xxSuccessful());
    } else {
      response = requestBuilder.andExpect(status().is(400)).andReturn();
      return;
    }
    response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(request().asyncNotStarted())
            .andExpect(content().string(""))
            .andReturn();

    var result =
        gaenDataService.getSortedExposedForKeyDate(
            now.atStartOfDay().minusDays(1).getTimestamp(),
            null,
            (now.getTimestamp() / releaseBucketDuration + 1) * releaseBucketDuration);
    assertEquals(2, result.size());
    for (var key : result) {
      assertEquals(Integer.valueOf(144), key.getRollingPeriod());
    }

    result =
        gaenDataService.getSortedExposedForKeyDate(
            now.atStartOfDay().minusDays(1).getTimestamp(),
            null,
            (now.getTimestamp() / releaseBucketDuration) * releaseBucketDuration);
    assertEquals(0, result.size());
  }

  @Test
  public void testAllKeysWrongButStill200() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();
    var requestList = new GaenRequest();
    var gaenKey1 = new GaenKey();

    gaenKey1.setRollingStartNumber((int) midnight.minusDays(30).get10MinutesSince1970());
    gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes01".getBytes("UTF-8")));
    gaenKey1.setRollingPeriod(0);
    gaenKey1.setFake(0);
    gaenKey1.setTransmissionRiskLevel(0);
    var gaenKey2 = new GaenKey();
    gaenKey2.setRollingStartNumber((int) midnight.minusDays(1).get10MinutesSince1970());
    gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes02".getBytes("UTF-8")));
    gaenKey2.setRollingPeriod(-10);
    gaenKey2.setFake(0);
    gaenKey2.setTransmissionRiskLevel(0);
    List<GaenKey> exposedKeys = new ArrayList<>();
    exposedKeys.add(gaenKey1);
    exposedKeys.add(gaenKey2);
    for (int i = 0; i < 12; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber((int) midnight.plusDays(10).get10MinutesSince1970());
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(0);
      tmpKey.setTransmissionRiskLevel(0);
      exposedKeys.add(tmpKey);
    }
    requestList.setGaenKeys(exposedKeys);
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    requestList.setDelayedKeyDate((int) duration);
    gaenKey1.setFake(0);
    String token = createToken(now.plusMinutes(5));
    MvcResult response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc.perform(asyncDispatch(response)).andExpect(status().is2xxSuccessful());
    response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(request().asyncNotStarted())
            .andExpect(content().string(""))
            .andReturn();

    var result =
        gaenDataService.getSortedExposedForKeyDate(
            midnight.minusDays(1).getTimestamp(),
            null,
            (now.getTimestamp() / releaseBucketDuration + 1) * releaseBucketDuration);
    // all keys are in compatible
    assertEquals(0, result.size());
  }

  @Transactional
  public void testMultipleKeyUpload() throws Exception {
    testNKeys(UTCInstant.now(), 14, true);
  }

  @Test
  @Transactional
  public void testCanUploadMoreThan14Keys() throws Exception {
    testNKeys(UTCInstant.now(), 30, true);
  }

  @Test
  @Transactional
  public void testCannotUploadMoreThan30Keys() throws Exception {
    testNKeys(UTCInstant.now(), 31, false);
    testNKeys(UTCInstant.now(), 100, false);
    testNKeys(UTCInstant.now(), 1000, false);
  }

  private Map<String, String> headers =
      Map.of(
          "X-Content-Type-Options",
          "nosniff",
          "X-Frame-Options",
          "DENY",
          "X-Xss-Protection",
          "1; mode=block");

  @Test
  public void testSecurityHeaders() throws Exception {
    MockHttpServletResponse response =
        mockMvc.perform(get("/v1")).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
    for (var header : headers.keySet()) {
      assertTrue(response.containsHeader(header));
      assertEquals(headers.get(header), response.getHeader(header));
    }
    var now = UTCInstant.now();
    var midnight = UTCInstant.today();
    response =
        mockMvc
            .perform(
                get("/v1/gaen/exposed/" + midnight.minusDays(8).getTimestamp())
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();
    for (var header : headers.keySet()) {
      assertTrue(response.containsHeader(header));
      assertEquals(headers.get(header), response.getHeader(header));
    }
  }

  @Test
  public void testUploadWithNegativeRollingPeriodFails() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();
    var requestList = new GaenRequest();
    var gaenKey1 = new GaenKey();
    gaenKey1.setRollingStartNumber((int) midnight.get10MinutesSince1970());
    gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    gaenKey1.setRollingPeriod(-1);
    gaenKey1.setFake(0);
    gaenKey1.setTransmissionRiskLevel(0);
    var gaenKey2 = new GaenKey();
    gaenKey2.setRollingStartNumber((int) midnight.minusDays(1).get10MinutesSince1970());
    gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    gaenKey2.setRollingPeriod(-5);
    gaenKey2.setFake(0);
    gaenKey2.setTransmissionRiskLevel(0);
    List<GaenKey> exposedKeys = new ArrayList<>();
    exposedKeys.add(gaenKey1);
    exposedKeys.add(gaenKey2);
    for (int i = 0; i < 12; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      exposedKeys.add(tmpKey);
    }
    requestList.setGaenKeys(exposedKeys);
    var duration = (int) midnight.plusDays(1).get10MinutesSince1970();
    requestList.setDelayedKeyDate((int) duration);
    gaenKey1.setFake(0);
    String token = createToken(now.plusMinutes(5));
    MvcResult response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc.perform(asyncDispatch(response)).andExpect(status().is(200));

    var result =
        gaenDataService.getSortedExposedForKeyDate(
            midnight.minusDays(1).getTimestamp(),
            null,
            (now.getTimestamp() / releaseBucketDuration + 1) * releaseBucketDuration);
    // all keys are invalid
    assertEquals(0, result.size());
    result =
        gaenDataService.getSortedExposedForKeyDate(
            midnight.getTimestamp(),
            null,
            (now.getTimestamp() / releaseBucketDuration + 1) * releaseBucketDuration);
    // all keys are invalid
    assertEquals(0, result.size());
  }

  @Test
  public void testMultipleKeyUploadFake() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    var requestList = new GaenRequest();
    var gaenKey1 = new GaenKey();
    gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    gaenKey1.setRollingStartNumber(
        (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
    gaenKey1.setRollingPeriod(144);
    gaenKey1.setTransmissionRiskLevel(0);
    var gaenKey2 = new GaenKey();
    gaenKey2.setRollingStartNumber(
        (int)
            Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
                .dividedBy(Duration.ofMinutes(10)));
    gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    gaenKey2.setRollingPeriod(144);
    gaenKey2.setTransmissionRiskLevel(0);

    gaenKey1.setFake(1);
    gaenKey2.setFake(1);

    List<GaenKey> exposedKeys = new ArrayList<>();
    exposedKeys.add(gaenKey1);
    exposedKeys.add(gaenKey2);
    for (int i = 0; i < 12; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      exposedKeys.add(tmpKey);
    }
    requestList.setGaenKeys(exposedKeys);
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    requestList.setDelayedKeyDate((int) duration);

    String token = createToken(true, now.plusMinutes(5));
    MvcResult responseAsync =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(responseAsync)).andExpect(status().is2xxSuccessful());
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/gaen/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void testMultipleKeyUploadFakeAllKeysNeedToBeFake() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    var requestList = new GaenRequest();
    var gaenKey1 = new GaenKey();
    gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    gaenKey1.setRollingStartNumber(
        (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
    gaenKey1.setRollingPeriod(144);
    gaenKey1.setTransmissionRiskLevel(0);
    var gaenKey2 = new GaenKey();
    gaenKey2.setRollingStartNumber(
        (int)
            Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
                .dividedBy(Duration.ofMinutes(10)));
    gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    gaenKey2.setRollingPeriod(144);
    gaenKey2.setTransmissionRiskLevel(0);

    gaenKey1.setFake(0);
    gaenKey2.setFake(1);

    List<GaenKey> exposedKeys = new ArrayList<>();
    exposedKeys.add(gaenKey1);
    exposedKeys.add(gaenKey2);
    for (int i = 0; i < 12; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      exposedKeys.add(tmpKey);
    }
    requestList.setGaenKeys(exposedKeys);
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    requestList.setDelayedKeyDate((int) duration);

    String token = createToken(true, now.plusMinutes(5));
    MvcResult responseAsync =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(responseAsync)).andExpect(status().is2xxSuccessful());
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/gaen/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void testMultipleKeyUploadFakeIfJWTNotFakeAllKeysCanBeFake() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    var requestList = new GaenRequest();
    var gaenKey1 = new GaenKey();
    gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    gaenKey1.setRollingStartNumber(
        (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
    gaenKey1.setRollingPeriod(144);
    gaenKey1.setTransmissionRiskLevel(0);
    var gaenKey2 = new GaenKey();
    gaenKey2.setRollingStartNumber(
        (int)
            Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
                .dividedBy(Duration.ofMinutes(10)));
    gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    gaenKey2.setRollingPeriod(144);
    gaenKey2.setTransmissionRiskLevel(0);

    gaenKey1.setFake(1);
    gaenKey2.setFake(1);

    List<GaenKey> exposedKeys = new ArrayList<>();
    exposedKeys.add(gaenKey1);
    exposedKeys.add(gaenKey2);
    for (int i = 0; i < 12; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      exposedKeys.add(tmpKey);
    }
    requestList.setGaenKeys(exposedKeys);
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    requestList.setDelayedKeyDate((int) duration);

    String token = createToken(false, now.plusMinutes(5));
    MvcResult responseAsync =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(responseAsync)).andExpect(status().is2xxSuccessful());
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/gaen/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void testMultipleKeyNonEmptyUpload() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    var requestList = new GaenRequest();
    List<GaenKey> exposedKeys = new ArrayList<GaenKey>();
    requestList.setGaenKeys(exposedKeys);
    String token = createToken(now.plusMinutes(5));
    MvcResult responseAsync =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(request().asyncNotStarted())
            .andExpect(status().is(400))
            .andReturn();

    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/gaen/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void testMultipleKeyNonNullUpload() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    var requestList = new GaenRequest();
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    requestList.setDelayedKeyDate((int) duration);

    String token = createToken(now.plusMinutes(5));
    MvcResult responseAsync =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(request().asyncNotStarted())
            .andExpect(status().is(400))
            .andReturn();

    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/v1/gaen/exposedlist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn()
            .getResponse();
  }

  @Test
  public void keyNeedsToBeBase64() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    exposeeRequest.setDelayedKeyDate((int) duration);
    GaenKey key = new GaenKey();
    key.setKeyData("00000000testKey32Bytes--");
    key.setRollingPeriod(144);
    key.setRollingStartNumber((int) now.get10MinutesSince1970());
    key.setTransmissionRiskLevel(1);
    key.setFake(0);
    List<GaenKey> keys = new ArrayList<>();
    keys.add(key);
    for (int i = 0; i < 13; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    exposeeRequest.setGaenKeys(keys);

    String token = createToken(false, now.plusMinutes(5));
    mockMvc
        .perform(
            post("/v1/gaen/exposed")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                .content(json(exposeeRequest)))
        .andExpect(status().is(400))
        .andReturn();
  }

  @Test
  public void testKeyDateBeforeOnsetIsNotInserted() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    exposeeRequest.setDelayedKeyDate((int) duration);
    GaenKey key = new GaenKey();
    key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    key.setRollingPeriod(144);
    key.setRollingStartNumber(
        (int)
            Duration.ofMillis(Instant.now().minus(Duration.ofDays(2)).toEpochMilli())
                .dividedBy(Duration.ofMinutes(10)));
    key.setTransmissionRiskLevel(1);
    key.setFake(0);
    List<GaenKey> keys = new ArrayList<>();
    keys.add(key);
    for (int i = 0; i < 13; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber((int) now.get10MinutesSince1970());
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    exposeeRequest.setGaenKeys(keys);

    String token =
        createToken(now.plusMinutes(5), now.getLocalDate().format(DateTimeFormatter.ISO_DATE));
    MvcResult response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(exposeeRequest)))
            .andExpect(request().asyncStarted())
            .andExpect(status().is(200))
            .andReturn();
    var result =
        gaenDataService.getSortedExposedForKeyDate(
            midnight.minusDays(2).getTimestamp(),
            null,
            (now.getTimestamp() / releaseBucketDuration + 1) * releaseBucketDuration);
    assertEquals(0, result.size());
  }

  @Test
  public void cannotUseExpiredToken() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    exposeeRequest.setDelayedKeyDate((int) duration);
    GaenKey key = new GaenKey();
    key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    key.setRollingPeriod(144);
    key.setRollingStartNumber(
        (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
    key.setTransmissionRiskLevel(1);
    key.setFake(1);
    List<GaenKey> keys = new ArrayList<>();
    keys.add(key);
    for (int i = 0; i < 13; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    exposeeRequest.setGaenKeys(keys);

    String token = createToken(now.minusMinutes(5));

    MvcResult response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(exposeeRequest)))
            .andExpect(request().asyncNotStarted())
            .andExpect(status().is4xxClientError())
            .andReturn();
  }

  @Test
  public void cannotUseKeyDateInFuture() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    exposeeRequest.setDelayedKeyDate((int) midnight.get10MinutesSince1970());
    GaenKey key = new GaenKey();
    key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes++".getBytes("UTF-8")));
    key.setRollingPeriod(144);
    key.setRollingStartNumber((int) midnight.plusDays(2).get10MinutesSince1970());
    key.setTransmissionRiskLevel(1);
    key.setFake(0);
    List<GaenKey> keys = new ArrayList<>();
    keys.add(key);
    for (int i = 0; i < 13; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes++".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    exposeeRequest.setGaenKeys(keys);

    String token = createToken(now.plusMinutes(5));

    MvcResult response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(exposeeRequest)))
            .andExpect(status().is(200))
            .andReturn();
    var result =
        gaenDataService.getSortedExposedForKeyDate(
            midnight.plusDays(2).getTimestamp(),
            null,
            (now.getTimestamp() / releaseBucketDuration + 1) * releaseBucketDuration);
    assertEquals(0, result.size());
  }

  @Test
  public void keyDateNotOlderThan21Days() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    exposeeRequest.setDelayedKeyDate((int) duration);
    GaenKey key = new GaenKey();
    key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    key.setRollingPeriod(144);
    key.setRollingStartNumber((int) midnight.minusDays(22).get10MinutesSince1970());
    key.setTransmissionRiskLevel(1);
    key.setFake(0);
    List<GaenKey> keys = new ArrayList<>();
    keys.add(key);
    for (int i = 0; i < 13; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    exposeeRequest.setGaenKeys(keys);

    String token = createToken(now.plusMinutes(5), "2020-01-01");

    MvcResult response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(exposeeRequest)))
            .andExpect(request().asyncStarted())
            .andExpect(status().is(200))
            .andReturn();
    var result =
        gaenDataService.getSortedExposedForKeyDate(
            midnight.minusDays(22).getTimestamp(),
            null,
            (now.getTimestamp() / releaseBucketDuration + 1) * releaseBucketDuration);
    assertEquals(0, result.size());
  }

  @Test
  public void cannotUseTokenWithWrongScope() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    var duration = midnight.plusDays(1).get10MinutesSince1970();
    exposeeRequest.setDelayedKeyDate((int) duration);
    GaenKey key = new GaenKey();
    key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    key.setRollingPeriod(144);
    key.setRollingStartNumber((int) now.get10MinutesSince1970());
    key.setTransmissionRiskLevel(1);
    key.setFake(1);
    List<GaenKey> keys = new ArrayList<>();
    keys.add(key);
    for (int i = 0; i < 13; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber((int) now.get10MinutesSince1970());
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    exposeeRequest.setGaenKeys(keys);

    String token = createTokenWithScope(now.plusMinutes(5), "not-exposed");

    MvcResult response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(exposeeRequest)))
            .andExpect(status().is(403))
            .andReturn();

    // Also for a 403 response, the token cannot be used a 2nd time
    response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(exposeeRequest)))
            .andExpect(request().asyncNotStarted())
            .andExpect(status().is(401))
            .andExpect(content().string(""))
            .andReturn();
  }

  @Test
  public void uploadKeysAndUploadKeyNextDay() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    List<GaenKey> keys = new ArrayList<>();
    for (int i = 0; i < 14; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int)
              Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
                  .dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(0);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    var delayedKeyDateSent = (int) midnight.get10MinutesSince1970();
    exposeeRequest.setDelayedKeyDate(delayedKeyDateSent);
    exposeeRequest.setGaenKeys(keys);
    String token = createToken(now.plusMinutes(5));
    MvcResult responseAsync =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(exposeeRequest)))
            .andExpect(request().asyncStarted())
            .andReturn();
    MockHttpServletResponse response =
        mockMvc
            .perform(asyncDispatch(responseAsync))
            .andExpect(status().is(200))
            .andReturn()
            .getResponse();
    assertTrue(
        response.containsHeader("Authorization"), "Authorization header not found in response");
    String jwtString = response.getHeader("Authorization").replace("Bearer ", "");
    Jwt jwtToken =
        Jwts.parserBuilder()
            .setSigningKey(keyVault.get("nextDayJWT").getPublic())
            .build()
            .parse(jwtString);
    GaenSecondDay secondDay = new GaenSecondDay();
    var tmpKey = new GaenKey();
    tmpKey.setRollingStartNumber(delayedKeyDateSent);
    tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    tmpKey.setRollingPeriod(144);
    tmpKey.setFake(0);
    tmpKey.setTransmissionRiskLevel(0);
    secondDay.setDelayedKey(tmpKey);
    responseAsync =
        mockMvc
            .perform(
                post("/v1/gaen/exposednextday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtString)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(secondDay)))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(responseAsync)).andExpect(status().is(200));
  }

  // @Test
  // TODO: Is this still a requirement? Currently the key just gets filtered out
  public void uploadKeysAndUploadKeyNextDayWithNegativeRollingPeriodFails() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    List<GaenKey> keys = new ArrayList<>();
    for (int i = 0; i < 14; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int)
              Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
                  .dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(0);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    var delayedKeyDateSent = (int) midnight.get10MinutesSince1970();
    exposeeRequest.setDelayedKeyDate(delayedKeyDateSent);
    exposeeRequest.setGaenKeys(keys);
    String token = createToken(now.plusMinutes(5));
    MvcResult responseAsync =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(exposeeRequest)))
            .andExpect(request().asyncStarted())
            .andReturn();
    MockHttpServletResponse response =
        mockMvc
            .perform(asyncDispatch(responseAsync))
            .andExpect(status().is(200))
            .andReturn()
            .getResponse();
    assertTrue(
        response.containsHeader("Authorization"), "Authorization header not found in response");
    String jwtString = response.getHeader("Authorization").replace("Bearer ", "");
    Jwt jwtToken =
        Jwts.parserBuilder()
            .setSigningKey(keyVault.get("nextDayJWT").getPublic())
            .build()
            .parse(jwtString);
    GaenSecondDay secondDay = new GaenSecondDay();
    var tmpKey = new GaenKey();
    tmpKey.setRollingStartNumber(delayedKeyDateSent);
    tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    tmpKey.setRollingPeriod(-1);
    tmpKey.setFake(0);
    tmpKey.setTransmissionRiskLevel(0);
    secondDay.setDelayedKey(tmpKey);
    responseAsync =
        mockMvc
            .perform(
                post("/v1/gaen/exposednextday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtString)
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                    .content(json(secondDay)))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(responseAsync)).andExpect(status().isBadRequest());
  }

  @Test
  public void delayedKeyDateBoundaryCheck() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    List<GaenKey> keys = new ArrayList<>();
    for (int i = 0; i < 14; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber((int) midnight.minusDays(1).get10MinutesSince1970());
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(0);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }

    Map<Integer, Boolean> tests =
        Map.of(
            -2, false,
            -1, true,
            0, true,
            1, true,
            2, false);

    for (Map.Entry<Integer, Boolean> t : tests.entrySet()) {
      Integer offset = t.getKey();
      Boolean pass = t.getValue();
      logger.info("Testing offset {} which should pass {}", offset, pass);
      var delayedKeyDateSent = (int) midnight.plusDays(offset).get10MinutesSince1970();
      exposeeRequest.setDelayedKeyDate(delayedKeyDateSent);
      exposeeRequest.setGaenKeys(keys);
      String token = createToken(now.plusMinutes(5));
      if (pass) {
        MvcResult responseAsync =
            mockMvc
                .perform(
                    post("/v1/gaen/exposed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header(
                            "User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                        .content(json(exposeeRequest)))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc
            .perform(asyncDispatch(responseAsync))
            .andExpect(status().is(200))
            .andReturn()
            .getResponse();
      } else {
        MvcResult responseAsync =
            mockMvc
                .perform(
                    post("/v1/gaen/exposed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header(
                            "User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                        .content(json(exposeeRequest)))
                .andExpect(status().is(400))
                .andReturn();
      }
    }
  }

  @Test
  public void testTokenValiditySurpassesMaxJwtValidity() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    GaenRequest exposeeRequest = new GaenRequest();
    List<GaenKey> keys = new ArrayList<>();
    for (int i = 0; i < 14; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber(
          (int)
              Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
                  .dividedBy(Duration.ofMinutes(10)));
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(0);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    exposeeRequest.setGaenKeys(keys);
    var delayedKeyDateSent = (int) midnight.plusDays(1).get10MinutesSince1970();
    exposeeRequest.setDelayedKeyDate(delayedKeyDateSent);
    int maxJWTValidityInMinutes = 60;
    String token = createToken(now.plusMinutes(maxJWTValidityInMinutes + 1));

    mockMvc
        .perform(
            post("/v1/gaen/exposed")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29")
                .content(json(exposeeRequest)))
        .andExpect(status().is(401));
  }

  @Test
  public void testDebugController() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    // insert two times 10 keys per day for the last 14 days, with different
    // received at. In total: 280 keys
    insertNKeysPerDay(midnight, 14, 10, midnight.minusDays(1), true);
    insertNKeysPerDay(midnight, 14, 10, midnight.minusHours(12), true);

    // Request keys which have been received in the last day, must be 280 in total.
    // This is the debug controller, which returns keys based on the 'received at',
    // not based on the key date. So this request should return all keys with
    // 'received at' of the last day.

    MockHttpServletResponse response =
        mockMvc
            .perform(
                get("/v1/debug/exposed/" + midnight.getTimestamp())
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();
    verifyZipInZipResponse(response, 280, 144);
  }

  @Test
  @Transactional
  public void zipContainsFiles() throws Exception {
    var now = UTCInstant.now();
    var clock = Clock.offset(Clock.systemUTC(), now.getDuration(now.atStartOfDay().plusHours(12)));
    UTCInstant.setClock(clock);
    now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    // Insert two times 5 keys per day for the last 14 days. the second batch has a
    // different 'received at' timestamp. (+12 hours compared to the first)
    insertNKeysPerDay(midnight, 14, 5, midnight.minusDays(1), false);
    insertNKeysPerDay(midnight, 14, 5, midnight.minusHours(12), false);

    // request the keys with key date 8 days ago. no publish until.
    MockHttpServletResponse response =
        mockMvc
            .perform(
                get("/v1/gaen/exposed/" + midnight.minusDays(8).getTimestamp())
                    .header("User-Agent", "MockMVC"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    Long publishedUntil = Long.parseLong(response.getHeader("X-PUBLISHED-UNTIL"));
    assertTrue(publishedUntil < now.getTimestamp(), "Published until must be in the past");

    // must contain 20 keys: 5 from the first insert, 5 from the second insert and
    // 10 random keys
    verifyZipResponse(response, 20, 144);

    // request again the keys with date date 8 days ago. with publish until, so that
    // we only get the second batch.
    var bucketAfterSecondRelease = midnight.minusHours(12);

    MockHttpServletResponse responseWithPublishedAfter =
        mockMvc
            .perform(
                get("/v1/gaen/exposed/" + midnight.minusDays(8).getTimestamp())
                    .header("User-Agent", "MockMVC")
                    .param(
                        "publishedafter", Long.toString(bucketAfterSecondRelease.getTimestamp())))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    // must contain 15 keys: 5 from the second insert and 10 random keys
    verifyZipResponse(responseWithPublishedAfter, 15, 144);
    UTCInstant.resetClock();
  }

  @Test
  @Transactional(transactionManager = "testTransactionManager")
  public void testNonEmptyResponseAnd304() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    MockHttpServletResponse response =
        mockMvc
            .perform(
                get("/v1/gaen/exposed/" + midnight.minusDays(8).getTimestamp())
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    verifyZipResponse(response, 10, 144);
  }

  @Test
  @Transactional(transactionManager = "testTransactionManager")
  public void testTodayWeDontHaveKeys() throws Exception {
    var midnight = UTCInstant.today();
    MockHttpServletResponse response =
        mockMvc
            .perform(
                get("/v1/gaen/exposed/" + midnight.getTimestamp()).header("User-Agent", "MockMVC"))
            .andExpect(status().is(204))
            .andReturn()
            .getResponse();
  }

  // @Test
  // @Transactional(transactionManager = "testTransactionManager")
  public void testEtag() throws Exception {
    var now = UTCInstant.now();
    var midnight = now.atStartOfDay();

    insertNKeysPerDay(midnight, 14, 10, midnight.minusDays(1), false);
    insertNKeysPerDay(midnight, 14, 10, midnight.minusHours(12), false);

    // request the keys with date date 1 day ago. no publish until.
    MockHttpServletResponse response =
        mockMvc
            .perform(
                get("/v1/gaen/exposed/" + midnight.minusDays(8).getTimestamp())
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    Long publishedUntil = Long.parseLong(response.getHeader("X-PUBLISHED-UNTIL"));
    assertTrue(publishedUntil < System.currentTimeMillis(), "Published until must be in the past");
    var expectedEtag = response.getHeader("etag");

    response =
        mockMvc
            .perform(
                get("/v1/gaen/exposed/" + midnight.minusDays(8).getTimestamp())
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    publishedUntil = Long.parseLong(response.getHeader("X-PUBLISHED-UNTIL"));
    assertTrue(publishedUntil < System.currentTimeMillis(), "Published until must be in the past");
    assertEquals(expectedEtag, response.getHeader("etag"));

    insertNKeysPerDay(midnight, 14, 10, midnight.minusHours(12), false);

    response =
        mockMvc
            .perform(
                get("/v1/gaen/exposed/" + midnight.minusDays(8).getTimestamp())
                    .header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    publishedUntil = Long.parseLong(response.getHeader("X-PUBLISHED-UNTIL"));
    assertTrue(publishedUntil < System.currentTimeMillis(), "Published until must be in the past");
    assertNotEquals(expectedEtag, response.getHeader("etag"));
  }

  @Test
  public void testMalciousTokenFails() throws Exception {
    var requestList = new GaenRequest();
    List<GaenKey> exposedKeys = new ArrayList<GaenKey>();
    requestList.setGaenKeys(exposedKeys);
    String token = createMaliciousToken(UTCInstant.now().plusMinutes(5));
    MvcResult response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(requestList)))
            .andExpect(request().asyncNotStarted())
            .andExpect(status().is(401))
            .andReturn();
    String authenticateError = response.getResponse().getHeader("www-authenticate");
    assertTrue(authenticateError.contains("Unsigned Claims JWTs are not supported."));
  }

  /** Verifies a zip in zip response, that each inner zip is again valid. */
  private void verifyZipInZipResponse(
      MockHttpServletResponse response, int expectKeyCount, int expectedRollingPeriod)
      throws Exception {
    ByteArrayInputStream baisOuter = new ByteArrayInputStream(response.getContentAsByteArray());
    ZipInputStream zipOuter = new ZipInputStream(baisOuter);
    ZipEntry entry = zipOuter.getNextEntry();
    while (entry != null) {
      ZipInputStream zipInner =
          new ZipInputStream(new ByteArrayInputStream(zipOuter.readAllBytes()));
      verifyKeyZip(zipInner, expectKeyCount, expectedRollingPeriod);
      entry = zipOuter.getNextEntry();
    }
  }

  /** Verifies a zip response, checks if keys and signature is correct. */
  private void verifyZipResponse(
      MockHttpServletResponse response, int expectKeyCount, int expectedRollingPeriod)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    ByteArrayInputStream baisZip = new ByteArrayInputStream(response.getContentAsByteArray());
    ZipInputStream keyZipInputstream = new ZipInputStream(baisZip);
    verifyKeyZip(keyZipInputstream, expectKeyCount, expectedRollingPeriod);
  }

  private void verifyKeyZip(
      ZipInputStream keyZipInputstream, int expectKeyCount, int expectedRollingPeriod)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    ZipEntry entry = keyZipInputstream.getNextEntry();
    boolean foundData = false;
    boolean foundSignature = false;

    byte[] signatureProto = null;
    byte[] exportBin = null;
    byte[] keyProto = null;

    while (entry != null) {
      if (entry.getName().equals("export.bin")) {
        foundData = true;
        exportBin = keyZipInputstream.readAllBytes();
        keyProto = new byte[exportBin.length - 16];
        System.arraycopy(exportBin, 16, keyProto, 0, keyProto.length);
      }
      if (entry.getName().equals("export.sig")) {
        foundSignature = true;
        signatureProto = keyZipInputstream.readAllBytes();
      }
      entry = keyZipInputstream.getNextEntry();
    }

    assertTrue(foundData, "export.bin not found in zip");
    assertTrue(foundSignature, "export.sig not found in zip");

    TEKSignatureList list = TemporaryExposureKeyFormat.TEKSignatureList.parseFrom(signatureProto);
    TemporaryExposureKeyExport export =
        TemporaryExposureKeyFormat.TemporaryExposureKeyExport.parseFrom(keyProto);
    for (var key : export.getKeysList()) {
      assertEquals(expectedRollingPeriod, key.getRollingPeriod());
    }
    var sig = list.getSignatures(0);
    java.security.Signature signatureVerifier =
        java.security.Signature.getInstance(sig.getSignatureInfo().getSignatureAlgorithm().trim());
    signatureVerifier.initVerify(signer.getPublicKey());

    signatureVerifier.update(exportBin);
    assertTrue(
        signatureVerifier.verify(sig.getSignature().toByteArray()),
        "Could not verify signature in zip file");
    assertEquals(expectKeyCount, export.getKeysCount());
  }

  /**
   * Creates keysPerDay for every day: lastDay, lastDay-1, ..., lastDay - daysBack + 1
   *
   * @param lastDay of the created keys
   * @param daysBack of the key creation, counted including the lastDay
   * @param keysPerDay that will be created for every day
   * @param receivedAt as sent to the DB
   * @param debug if true, inserts the keys in the debug table.
   */
  private void insertNKeysPerDay(
      UTCInstant lastDay, int daysBack, int keysPerDay, UTCInstant receivedAt, boolean debug) {
    SecureRandom random = new SecureRandom();
    for (int d = 0; d < daysBack; d++) {
      var currentKeyDate = lastDay.minusDays(d);
      int currentRollingStartNumber = (int) currentKeyDate.get10MinutesSince1970();
      List<GaenKey> keys = new ArrayList<>();
      for (int n = 0; n < keysPerDay; n++) {
        GaenKey key = new GaenKey();
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        key.setKeyData(Base64.getEncoder().encodeToString(keyBytes));
        key.setRollingPeriod(144);
        key.setRollingStartNumber(currentRollingStartNumber);
        key.setTransmissionRiskLevel(1);
        key.setFake(0);
        keys.add(key);
      }
      if (debug) {
        testGaenDataService.upsertExposeesDebug(keys, receivedAt);
      } else {
        testGaenDataService.upsertExposees(keys, receivedAt);
      }
    }
  }
}
