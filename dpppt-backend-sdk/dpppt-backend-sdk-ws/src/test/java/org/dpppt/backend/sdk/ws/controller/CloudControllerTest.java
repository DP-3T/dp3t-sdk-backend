/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.commons.io.IOUtils;
import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.ws.filter.ResponseWrapperFilter;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
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
import io.jsonwebtoken.SignatureAlgorithm;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    "vcap.services.ecdsa_dev.credentials.publicKey=MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEc6n1FNz6RnwNeNM9H+KxaPckrBxgKU799v+DTy8ivc1ZM3nDyXq5zU2AFXvgvLFzWxU9z9FCcDPGTcN7cvOyXw==",
    "vcap.services.ecdsa_dev.credentials.privateKey=MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgx18xx3XadMzGCunnoUjpiCt1rZ81I4XAJbRaQi0eZbKgCgYIKoZIzj0DAQehRANCAARzqfUU3PpGfA140z0f4rFo9ySsHGApTv32/4NPLyK9zVkzecPJernNTYAVe+C8sXNbFT3P0UJwM8ZNw3ty87Jf"
})
public class CloudControllerTest {
    protected MockMvc mockMvc;
	@Autowired
	private WebApplicationContext webApplicationContext;
	protected ObjectMapper objectMapper;

    @Autowired
    private ResponseWrapperFilter filter;
    private  String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEc6n1FNz6RnwNeNM9H+KxaPckrBxgKU799v+DTy8ivc1ZM3nDyXq5zU2AFXvgvLFzWxU9z9FCcDPGTcN7cvOyXw==";

	@Before
	public void setup() throws Exception {
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
        Jwt jwt = Jwts.parserBuilder().setSigningKey(loadPublicKeyFromString()).build().parse(signature);
        Claims claims = (Claims)jwt.getBody();
        assertEquals("dp3t", claims.get("iss"));
    }

	protected String json(Object o) throws IOException {
		return objectMapper.writeValueAsString(o);
    }
    
    private PublicKey loadPublicKeyFromString() {
		X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
		try {
			KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
			return (PublicKey) kf.generatePublic(keySpecX509);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
    
}