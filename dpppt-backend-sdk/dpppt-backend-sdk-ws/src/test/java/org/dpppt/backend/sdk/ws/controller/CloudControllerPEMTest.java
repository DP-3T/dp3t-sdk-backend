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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.security.PublicKey;
import org.dpppt.backend.sdk.ws.filter.ResponseWrapperFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test-cloud"})
@TestPropertySource(
    properties = {
      "ws.app.source=org.dpppt.demo",
      "ws.origin.country=CH",
      "vcap.services.ecdsa_dev.credentials.publicKey=LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tDQpNSUlCUURDQjdxQURBZ0VDQWdqeStkZ0QxN2ZYTkRBS0JnZ3Foa2pPUFFRREFqQXFNUXN3Q1FZRFZRUUdFd0pEDQpTREVOTUFzR0ExVUVDQXdFUW1WeWJqRU1NQW9HQTFVRUNnd0RRa2xVTUI0WERUSXlNVEl3TVRBM05EazBNRm9YDQpEVEl6TURNeE1UQTNORGswTUZvd0tqRUxNQWtHQTFVRUJoTUNRMGd4RFRBTEJnTlZCQWdNQkVKbGNtNHhEREFLDQpCZ05WQkFvTUEwSkpWREJaTUJNR0J5cUdTTTQ5QWdFR0NDcUdTTTQ5QXdFSEEwSUFCRks3em9RdTZqVERibllyDQpmcnN5R3l3S0NUV2UxVnpTcnBuZ0JYTmd4NDM5QkVHSW02enF2LytHdDNtcGgrOS8xZG5ROWpWSVJTdC9SYWVwDQpHVjJOZ2dBd0NnWUlLb1pJemowRUF3SURRUUFEY3VFaXgrQURaWUp3K3M2TkgxSzdpNWVmQjJIM25HWkM1R0JhDQpVZHJWWk1uU0V2SlhIYmFXak5xRVQrZmJWSjFkK2pCU3RKL0pEVnFxQWQxMzYxeHQNCi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0NCg==",
      "vcap.services.ecdsa_dev.credentials.privateKey=LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tDQpNSUdIQWdFQU1CTUdCeXFHU000OUFnRUdDQ3FHU000OUF3RUhCRzB3YXdJQkFRUWdpSFNtMXdXY0Y1M2EzazRIDQppcEliN28rM0gzT3h6ZHRqK2JVWXV2eE56VnloUkFOQ0FBUlN1ODZFTHVvMHcyNTJLMzY3TWhzc0NnazFudFZjDQowcTZaNEFWellNZU4vUVJCaUp1czZyLy9ocmQ1cVlmdmY5WFowUFkxU0VVcmYwV25xUmxkallJQQ0KLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLQ0K"
    })
public class CloudControllerPEMTest {
  protected MockMvc mockMvc;
  @Autowired private WebApplicationContext webApplicationContext;
  protected ObjectMapper objectMapper;

  @Autowired private ResponseWrapperFilter filter;
  private PublicKey publicKey;

  @Before
  public void setup() throws Exception {
    this.publicKey = filter.getPublicKey();
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilter(filter, "/*").build();
    this.objectMapper = new ObjectMapper(new JsonFactory());
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  public void testHelloEnpointSignature() throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(get("/v1/gaen"))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse();
    String content = response.getContentAsString();
    assertEquals("Hello from DP3T WS", content);
    assertEquals("dp3t", response.getHeader("X-HELLO"));
    String signature = response.getHeader("Signature");
    @SuppressWarnings("rawtypes")
    Jwt jwt = Jwts.parserBuilder().setSigningKey(publicKey).build().parse(signature);
    Claims claims = (Claims) jwt.getBody();
    assertEquals("dp3t", claims.get("iss"));
  }

  protected String json(Object o) throws IOException {
    return objectMapper.writeValueAsString(o);
  }
}
