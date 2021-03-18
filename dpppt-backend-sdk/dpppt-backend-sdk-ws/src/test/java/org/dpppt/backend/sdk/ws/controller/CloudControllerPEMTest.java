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
      "vcap.services.ecdsa_dev.credentials.publicKey=LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNTRENDQWU2Z0F3SUJBZ0lVS3pEQlJIZlZVN2djRGZxUTBGUDFLTkJULzVJd0NnWUlLb1pJemowRUF3SXcKZXpFTE1Ba0dBMVVFQmhNQ1EwZ3hEVEFMQmdOVkJBZ01CRUpsY200eERUQUxCZ05WQkFjTUJFSmxjbTR4RERBSwpCZ05WQkFvTUEwSkpWREVNTUFvR0ExVUVDd3dEUlZkS01RMHdDd1lEVlFRRERBUlVaWE4wTVNNd0lRWUpLb1pJCmh2Y05BUWtCRmhSemRYQndiM0owUUdKcGRDNWhaRzFwYmk1amFEQWVGdzB5TURBME1qZ3hNakUwTlRGYUZ3MHkKTVRBME1qZ3hNakUwTlRGYU1Ic3hDekFKQmdOVkJBWVRBa05JTVEwd0N3WURWUVFJREFSQ1pYSnVNUTB3Q3dZRApWUVFIREFSQ1pYSnVNUXd3Q2dZRFZRUUtEQU5DU1ZReEREQUtCZ05WQkFzTUEwVlhTakVOTUFzR0ExVUVBd3dFClZHVnpkREVqTUNFR0NTcUdTSWIzRFFFSkFSWVVjM1Z3Y0c5eWRFQmlhWFF1WVdSdGFXNHVZMmd3VmpBUUJnY3EKaGtqT1BRSUJCZ1VyZ1FRQUNnTkNBQVRhc0ZoMEdLZWs1WTRKdXdnaTVIOEFrL3FmamtKQ3d6N1BYb0lVZWJnaQpzeTdUVlFMckltQlBVN2lnMDM3azBkb1V4aytYZEJLWDQzdi9yZEdZVUtmMW8xTXdVVEFkQmdOVkhRNEVGZ1FVCnVTS2lWSUdsRnpQdDdXd3Z1VGNicDNrckQ0UXdId1lEVlIwakJCZ3dGb0FVdVNLaVZJR2xGelB0N1d3dnVUY2IKcDNrckQ0UXdEd1lEVlIwVEFRSC9CQVV3QXdFQi96QUtCZ2dxaGtqT1BRUURBZ05JQURCRkFpQkRteEJUQ3BZawphN0hFeUFEWnN4d3p3b2h0TjBwNTd5QllMYjZzQ3B3ODhBSWhBSXpTUDdCV0tGWmNDSmI5ZmhwcjZaTXpJd0tlCkhhSWpIK2E4elV2Nk1PaW8KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=",
      "vcap.services.ecdsa_dev.credentials.privateKey=LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JR0VBZ0VBTUJBR0J5cUdTTTQ5QWdFR0JTdUJCQUFLQkcwd2F3SUJBUVFnMkRsai9lNW5rRlBtTk1MVjd1NjQKenFuOHdSeVgrUTgyc045RDRSWXlvNjJoUkFOQ0FBVGFzRmgwR0tlazVZNEp1d2dpNUg4QWsvcWZqa0pDd3o3UApYb0lVZWJnaXN5N1RWUUxySW1CUFU3aWcwMzdrMGRvVXhrK1hkQktYNDN2L3JkR1lVS2YxCi0tLS0tRU5EIFBSSVZBVEUgS0VZLS0tLS0K"
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
