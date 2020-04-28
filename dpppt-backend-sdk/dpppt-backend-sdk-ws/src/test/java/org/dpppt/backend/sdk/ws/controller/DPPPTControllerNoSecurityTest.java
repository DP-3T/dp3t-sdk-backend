/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import org.dpppt.backend.sdk.model.ExposeeAuthData;
import org.dpppt.backend.sdk.model.ExposeeRequest;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class DPPPTControllerNoSecurityTest extends BaseControllerNoSecurityTest {
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
    }
    @Test
    public void keyDateNotOlderThan21Days() throws Exception {
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusDays(22).toInstant().toEpochMilli());
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));

        MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is4xxClientError()).andReturn().getResponse();
    }
    @Test
    public void keyDateNotInTheFuture() throws Exception {
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(1).toInstant().toEpochMilli());
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));

        MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is4xxClientError()).andReturn().getResponse();
    }
    @Test
    public void justNowShouldBeFine() throws Exception {
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));

        MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "MockMVC")
                    .content(json(exposeeRequest)))
            .andExpect(status().is2xxSuccessful()).andReturn().getResponse();
    }
}