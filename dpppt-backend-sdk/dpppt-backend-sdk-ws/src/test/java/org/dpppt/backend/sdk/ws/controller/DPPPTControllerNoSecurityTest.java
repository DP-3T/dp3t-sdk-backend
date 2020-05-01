/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Iterator;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.dpppt.backend.sdk.model.ExposeeAuthData;
import org.dpppt.backend.sdk.model.ExposeeRequest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DPPPTControllerNoSecurityTest extends BaseControllerNoSecurityTest {
    @Test
    public void testJWT() throws Exception {
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setKeyDate(LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));
        exposeeRequest.setIsFake(0);
        MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .header("User-Agent", "MockMVC")
                                                            .content(json(exposeeRequest)))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse();
    }
    @Test
    public void testJWTFake() throws Exception {
        ExposeeRequest exposeeRequest = new ExposeeRequest();
        exposeeRequest.setAuthData(new ExposeeAuthData());
        exposeeRequest.setKeyDate(LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());
        exposeeRequest.setKey(Base64.getEncoder().encodeToString("test".getBytes("UTF-8")));
        exposeeRequest.setIsFake(1);
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

    @Autowired
    ObjectMapper mapper;

    @Test
    public void testBucketsAndExposedResponse() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/v1/buckets/2020-04-29")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .header("User-Agent", "MockMVC"))
                                        .andExpect(status().is(200))
                                        .andReturn().getResponse();
        Long bucket = mapper.readTree(response.getContentAsString()).get("buckets").elements().next().asLong();
        response = mockMvc.perform(get("/v1/exposed/" + bucket.toString()))
                    .andExpect(status().is2xxSuccessful()).andReturn().getResponse();
        assertEquals(response.getContentType(), "application/x-protobuf");
        response = mockMvc.perform(get("/v1/exposedjson/" + bucket.toString()))
        .andExpect(status().is2xxSuccessful()).andReturn().getResponse();
        assertEquals(response.getContentType(), "application/json");
    }
    @Test
    public void test400WhenNotModBatch() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/v1/buckets/2020-04-29")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .header("User-Agent", "MockMVC"))
                                        .andExpect(status().is(200))
                                        .andReturn().getResponse();
        Long bucket = mapper.readTree(response.getContentAsString()).get("buckets").elements().next().asLong() +1;

        response = mockMvc.perform(get("/v1/exposed/" + Long.toString(bucket)))
                    .andExpect(status().isBadRequest()).andReturn().getResponse();
        response = mockMvc.perform(get("/v1/exposedjson/" + Long.toString(bucket)))
        .andExpect(status().isBadRequest()).andReturn().getResponse();
    }
    @Test
    public void test404() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/v1/buckets/2020-04-29")
                                                            .contentType(MediaType.APPLICATION_JSON)
                                                            .header("User-Agent", "MockMVC"))
                                        .andExpect(status().is(200))
                                        .andReturn().getResponse();
        Iterator<JsonNode> buckets = mapper.readTree(response.getContentAsString()).get("buckets").elements();
        Long first = buckets.next().asLong();
        Long next = buckets.next().asLong();
        Long batchLength = next-first;
        long future = (long)Math.floor(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusDays(1).toInstant().toEpochMilli() / batchLength) * batchLength ;
        long past = (long)Math.floor(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusYears(1).toInstant().toEpochMilli() / batchLength) * batchLength ;

        response = mockMvc.perform(get("/v1/exposed/" + Long.toString(future)))
        .andExpect(status().isNotFound()).andReturn().getResponse();
        response = mockMvc.perform(get("/v1/exposedjson/" + Long.toString(future)))
        .andExpect(status().isNotFound()).andReturn().getResponse();

        response = mockMvc.perform(get("/v1/exposed/" + Long.toString(past)))
        .andExpect(status().isNotFound()).andReturn().getResponse();
        response = mockMvc.perform(get("/v1/exposedjson/" + Long.toString(past)))
        .andExpect(status().isNotFound()).andReturn().getResponse();
    }
}