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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.ws.security.KeyVault;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
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
}
