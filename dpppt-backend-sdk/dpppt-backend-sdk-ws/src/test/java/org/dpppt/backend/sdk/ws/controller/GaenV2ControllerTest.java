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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.model.gaen.GaenV2UploadKeysRequest;
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
public class GaenV2ControllerTest extends BaseControllerTest {
  @Autowired ProtoSignature signer;
  @Autowired KeyVault keyVault;
  @Autowired GAENDataService gaenDataService;

  Duration releaseBucketDuration = Duration.ofMillis(7200000L);

  private static final Logger logger = LoggerFactory.getLogger(GaenV2ControllerTest.class);

  @Test
  public void testHello() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(get("/v2/gaen"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();

    assertNotNull(response);
    assertEquals("Hello from DP3T WS GAEN V2", response.getContentAsString());
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
                post("/v2/gaen/exposed")
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

  @Test
  public void testUploadTodaysKeyWillBeReleasedTomorrow() throws Exception {
    var now = UTCInstant.now();
    GaenV2UploadKeysRequest exposeeRequest = new GaenV2UploadKeysRequest();
    List<GaenKey> keys = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber((int) now.atStartOfDay().minusDays(i).get10MinutesSince1970());
      var keyData = String.format("testKey32Bytes%02d", i);
      tmpKey.setKeyData(Base64.getEncoder().encodeToString(keyData.getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(0);
      tmpKey.setTransmissionRiskLevel(0);
      keys.add(tmpKey);
    }
    exposeeRequest.setGaenKeys(keys);

    String token = createToken(now.plusMinutes(5));
    MvcResult responseAsync =
        mockMvc
            .perform(
                post("/v2/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", androidUserAgent)
                    .content(json(exposeeRequest)))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(responseAsync)).andExpect(status().isOk());

    MockHttpServletResponse response =
        mockMvc
            .perform(get("/v2/gaen/exposed").header("User-Agent", androidUserAgent))
            .andExpect(status().is(204))
            .andReturn()
            .getResponse();

    Clock fourAMTomorrow =
        Clock.fixed(now.atStartOfDay().plusDays(1).plusHours(4).getInstant(), ZoneOffset.UTC);

    try (var timeLock = UTCInstant.setClock(fourAMTomorrow)) {
      response =
          mockMvc
              .perform(get("/v2/gaen/exposed").header("User-Agent", androidUserAgent))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse();
      verifyZipResponse(response, 15, 144);
    }
  }
}
