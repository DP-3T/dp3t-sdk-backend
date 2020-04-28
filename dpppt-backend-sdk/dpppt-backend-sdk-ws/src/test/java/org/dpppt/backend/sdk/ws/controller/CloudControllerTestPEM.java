/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.ws.filter.ResponseWrapperFilter;
import org.junit.Before;
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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.security.PublicKey;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;


import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dpppt.backend.sdk.model.ExposeeAuthData;
import org.junit.Test;

import org.springframework.http.MediaType;



@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test-cloud" })
@TestPropertySource(properties = 
{ 
    "ws.app.source=org.dpppt.demo",
    "vcap.services.ecdsa_dev.credentials.publicKey=-----BEGIN CERTIFICATE-----\\nMIICTDCCAfGgAwIBAgIUVtcQNpj/y1HxuvzmSTIKGxWo83swCgYIKoZIzj0EAwIw\\nezELMAkGA1UEBhMCQ0gxDTALBgNVBAgMBEJlcm4xDTALBgNVBAcMBEJlcm4xDDAK\\nBgNVBAoMA0JJVDEMMAoGA1UECwwDRVdKMQ0wCwYDVQQDDAREUDNUMSMwIQYJKoZI\\nhvcNAQkBFhRzdXBwb3J0QGJpdC5hZG1pbi5jaDAeFw0yMDA0MjgxMzE4NDhaFw0z\\nMDA0MjYxMzE4NDhaMHsxCzAJBgNVBAYTAkNIMQ0wCwYDVQQIDARCZXJuMQ0wCwYD\\nVQQHDARCZXJuMQwwCgYDVQQKDANCSVQxDDAKBgNVBAsMA0VXSjENMAsGA1UEAwwE\\nRFAzVDEjMCEGCSqGSIb3DQEJARYUc3VwcG9ydEBiaXQuYWRtaW4uY2gwWTATBgcq\\nhkjOPQIBBggqhkjOPQMBBwNCAATp9S2pd4Ib0oSr+CvkEnsXc8lwXUmwBV6GOfws\\nbBGQSt7T90WnySONeNiRZqW0NkV3DesJPhTPv2oZmuvfJKWbo1MwUTAdBgNVHQ4E\\nFgQUrY3JNpWDqtAiLdpcFDNKJXqkNv4wHwYDVR0jBBgwFoAUrY3JNpWDqtAiLdpc\\nFDNKJXqkNv4wDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEA9sVT\\ntBJIa2ufdTNGOcgKxexDC/ZBYCQ7Oj/qO7npuwYCIQCITgWqlPpG2Eepi/FzdtqN\\nuEWUpgNAIYoHR4dvutCsDQ==\\n-----END CERTIFICATE-----\\n",
    "vcap.services.ecdsa_dev.credentials.privateKey=-----BEGIN PRIVATE KEY-----\\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgWA4n+zROVUV/vaCR\\nlpq/Iqi7Cl7h+ZdEf/c/kMlN0jqhRANCAATp9S2pd4Ib0oSr+CvkEnsXc8lwXUmw\\nBV6GOfwsbBGQSt7T90WnySONeNiRZqW0NkV3DesJPhTPv2oZmuvfJKWb\\n-----END PRIVATE KEY-----\\n"
})
public class CloudControllerTestPEM {
    protected MockMvc mockMvc;
	@Autowired
	private WebApplicationContext webApplicationContext;
	protected ObjectMapper objectMapper;

    @Autowired
    private ResponseWrapperFilter filter;
    //private  String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEc6n1FNz6RnwNeNM9H+KxaPckrBxgKU799v+DTy8ivc1ZM3nDyXq5zU2AFXvgvLFzWxU9z9FCcDPGTcN7cvOyXw==";
    private PublicKey publicKey;
	@Before
	public void setup() throws Exception {
        this.publicKey = filter.getPublicKey();
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilter(filter, "/*").build();
		this.objectMapper = new ObjectMapper(new JsonFactory());
		this.objectMapper.registerModule(new JavaTimeModule());
		
    }
    
    @Test
    public void testJWT() throws Exception {
        
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setKeyDate(LocalDate.parse("2020-04-10").atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));
        MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .header("User-Agent", "MockMVC")
                                                            .content(json(exposeeRequest)))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse();
        response = mockMvc.perform(get("/v1/")).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
        String content = response.getContentAsString();
        assertEquals("Hello from DP3T WS",content);
        assertEquals("dp3t", response.getHeader("X-HELLO"));
        String signature = response.getHeader("Signature");
        Jwt jwt = Jwts.parserBuilder().setSigningKey(publicKey).build().parse(signature);
        Claims claims = (Claims)jwt.getBody();
        assertEquals("dp3t", claims.get("iss"));
    }

	protected String json(Object o) throws IOException {
		return objectMapper.writeValueAsString(o);
    }
}