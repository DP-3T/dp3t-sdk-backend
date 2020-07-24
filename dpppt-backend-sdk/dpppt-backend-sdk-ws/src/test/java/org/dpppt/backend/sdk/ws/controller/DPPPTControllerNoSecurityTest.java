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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;

import org.dpppt.backend.sdk.model.ExposeeAuthData;
import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = { "ws.gaen.randomkeysenabled=true" })
public class DPPPTControllerNoSecurityTest extends BaseControllerNoSecurityTest {
	@Test
	public void testJWT() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest.setKeyDate(UTCInstant.midnight().getTimestamp());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(0);
		MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
				.header("User-Agent", "MockMVC").content(json(exposeeRequest))).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
	}

	@Test
	public void testJWTFake() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest.setKeyDate(UTCInstant.midnight().getTimestamp());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(1);
		MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
				.header("User-Agent", "MockMVC").content(json(exposeeRequest))).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
	}

	@Test
	public void keyDateNotOlderThan21Days() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest.setKeyDate(UTCInstant.midnight().minusDays(21).getTimestamp());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
	}

	@Test
	public void keyDateNotInTheFuture() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest.setKeyDate(UTCInstant.midnight().plusDays(1).plusMinutes(1).getTimestamp());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
	}

	@Test
	public void justNowShouldBeFine() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest
				.setKeyDate(UTCInstant.midnight().getTimestamp());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));

		MockHttpServletResponse response = mockMvc.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
				.header("User-Agent", "MockMVC").content(json(exposeeRequest))).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
	}

	@Autowired
	ObjectMapper mapper;

	@Test
	public void testBucketsAndExposedResponse() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(
				get("/v1/buckets/"+getDateStringOfYesterday()).contentType(MediaType.APPLICATION_JSON).header("User-Agent", "MockMVC"))
				.andExpect(status().is(200)).andReturn().getResponse();
		Long bucket = mapper.readTree(response.getContentAsString()).get("buckets").elements().next().asLong();
		response = mockMvc.perform(get("/v1/exposed/" + bucket.toString())).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
		assertEquals(response.getContentType(), "application/x-protobuf");
		response = mockMvc.perform(get("/v1/exposedjson/" + bucket.toString())).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
		assertEquals(response.getContentType(), "application/json");
	}

	@Test
	public void test400WhenNotModBatch() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(
				get("/v1/buckets/"+getDateStringOfYesterday()).contentType(MediaType.APPLICATION_JSON).header("User-Agent", "MockMVC"))
				.andExpect(status().is(200)).andReturn().getResponse();
		Long bucket = mapper.readTree(response.getContentAsString()).get("buckets").elements().next().asLong() + 1;

		response = mockMvc.perform(get("/v1/exposed/" + Long.toString(bucket))).andExpect(status().isBadRequest())
				.andReturn().getResponse();
		response = mockMvc.perform(get("/v1/exposedjson/" + Long.toString(bucket))).andExpect(status().isBadRequest())
				.andReturn().getResponse();
	}

	@Test
	public void test404() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(
				get("/v1/buckets/"+getDateStringOfYesterday()).contentType(MediaType.APPLICATION_JSON).header("User-Agent", "MockMVC"))
				.andExpect(status().is(200)).andReturn().getResponse();
		Iterator<JsonNode> buckets = mapper.readTree(response.getContentAsString()).get("buckets").elements();
		Long first = buckets.next().asLong();
		Long next = buckets.next().asLong();
		Long releaseBucketDuration = next - first;
		long future = (long) Math
				.floor(UTCInstant.now().plusDays(1).getTimestamp()
						/ releaseBucketDuration)
				* releaseBucketDuration;
		long past = (long) Math.floor(
				UTCInstant.now().minusYears(1).getTimestamp()
						/ releaseBucketDuration)
				* releaseBucketDuration;

		response = mockMvc.perform(get("/v1/exposed/" + Long.toString(future))).andExpect(status().isNotFound())
				.andReturn().getResponse();
		response = mockMvc.perform(get("/v1/exposedjson/" + Long.toString(future))).andExpect(status().isNotFound())
				.andReturn().getResponse();

		response = mockMvc.perform(get("/v1/exposed/" + Long.toString(past))).andExpect(status().isNotFound())
				.andReturn().getResponse();
		response = mockMvc.perform(get("/v1/exposedjson/" + Long.toString(past))).andExpect(status().isNotFound())
				.andReturn().getResponse();
	}

	private String getDateStringOfYesterday(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date yesterday = UTCInstant.midnight().minusDays(1).getDate();
		return sdf.format(yesterday);
	}
}