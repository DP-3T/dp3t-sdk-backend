/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.dpppt.backend.sdk.model.ExposeeAuthData;
import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;


@SpringBootTest(properties =
{
    "ws.app.jwt.publickey=classpath://generated_pub.pem"
 })
public class DPPPTControllerTest extends BaseControllerTest {
    @Test
    public void testHello() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/v1"))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse();

        assertNotNull(response);
        assertEquals("Hello from DP3T WS", response.getContentAsString());
    }

    @Test
    public void testJWT() throws Exception {
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setOnset("2020-04-10");
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));
        String token = createToken(DateTime.now().plusMinutes(5));
        MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .header("Authorization", "Bearer " + createToken(DateTime.now().plusMinutes(5)))
                                                            .header("User-Agent", "MockMVC")
                                                            .content(json(exposeeRequest)))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse();
        response = mockMvc.perform(post("/v1/exposed")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + jwtToken)
                                .header("User-Agent", "MockMVC")
                                .content(json(exposeeRequest)))
                .andExpect(status().is4xxClientError()).andReturn().getResponse();
    }

    @Test
    public void cannotUseSameTokenTwice() throws Exception {
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setOnset("2020-04-10");
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));
        String token = createToken(DateTime.now().plusMinutes(5));

        MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is2xxSuccessful()).andReturn().getResponse();

        response = mockMvc.perform(post("/v1/exposed")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "MockMVC")
                .content(json(exposeeRequest)))
        .andExpect(status().is4xxClientError()).andReturn().getResponse();
    }

    @Test
    public void cannotUseExpiredToken() throws Exception {
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setOnset("2020-04-10");
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));
        String token = createToken(DateTime.now().minusMinutes(5));

        MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "MockMVC")
                .content(json(exposeeRequest)))
        .andExpect(status().is4xxClientError()).andReturn().getResponse();
    }
}