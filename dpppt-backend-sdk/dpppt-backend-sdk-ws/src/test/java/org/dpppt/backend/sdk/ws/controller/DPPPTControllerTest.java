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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.dpppt.backend.sdk.model.ExposedKey;
import org.dpppt.backend.sdk.model.ExposeeAuthData;
import org.dpppt.backend.sdk.model.ExposeeRequest;
import org.dpppt.backend.sdk.model.ExposeeRequestList;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

@SpringBootTest(properties = { "ws.app.jwt.publickey=classpath://generated_pub.pem", "ws.gaen.randomkeysenabled=true" })
public class DPPPTControllerTest extends BaseControllerTest {
	@Test
	public void testHello() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get("/v1")).andExpect(status().is2xxSuccessful()).andReturn()
				.getResponse();

		assertNotNull(response);
		assertEquals("Hello from DP3T WS", response.getContentAsString());
	}

	@Test
	public void testJWT() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest
				.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(0);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(
				post("/v1/exposed").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC").content(json(exposeeRequest)))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse();
		response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();

	}

	@Test
	public void testMultipleKeyUpload() throws Exception {
		var requestList = new ExposeeRequestList();
		var exposedKey1 = new ExposedKey();
		exposedKey1.setKeyDate(OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposedKey1.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		var exposedKey2 = new ExposedKey();
		exposedKey2.setKeyDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).toInstant().toEpochMilli());
		exposedKey2.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		List<ExposedKey> exposedKeys = List.of(exposedKey1, exposedKey2);
		requestList.setExposedKeys(exposedKeys);
		requestList.setFake(0);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/exposedlist")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
		response = mockMvc
				.perform(post("/v1/exposedlist").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void testMultipleKeyFakeUpload() throws Exception {
		var requestList = new ExposeeRequestList();
		var exposedKey1 = new ExposedKey();
		exposedKey1.setKeyDate(OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposedKey1.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		var exposedKey2 = new ExposedKey();
		exposedKey2.setKeyDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).toInstant().toEpochMilli());
		exposedKey2.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		List<ExposedKey> exposedKeys = List.of(exposedKey1, exposedKey2);
		requestList.setExposedKeys(exposedKeys);
		requestList.setFake(1);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/exposedlist")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
		response = mockMvc
				.perform(post("/v1/exposedlist").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void testMultipleKeyNonEmptyUpload() throws Exception {
		var requestList = new ExposeeRequestList();
		List<ExposedKey> exposedKeys = new ArrayList<ExposedKey>();
		requestList.setExposedKeys(exposedKeys);
		requestList.setFake(0);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/exposedlist")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is(400)).andReturn()
				.getResponse();
		response = mockMvc
				.perform(post("/v1/exposedlist").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void testMultipleKeyNonNullUpload() throws Exception {
		var requestList = new ExposeeRequestList();
		List<ExposedKey> exposedKeys = new ArrayList<ExposedKey>();
		requestList.setFake(0);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/exposedlist")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is(400)).andReturn()
				.getResponse();
		response = mockMvc
				.perform(post("/v1/exposedlist").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void keyNeedsToBeBase64() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest
				.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey("testKey32Bytes--testKey32Bytes--");
		exposeeRequest.setIsFake(1);
		String token = createToken(true, OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(
				post("/v1/exposed").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC").content(json(exposeeRequest)))
				.andExpect(status().is(400)).andReturn().getResponse();
	}

	@Test
	public void testJWTFake() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest
				.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(1);
		String token = createToken(true, OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(
				post("/v1/exposed").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC").content(json(exposeeRequest)))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse();
		response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void cannotUseKeyDateBeforeOnset() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest.setKeyDate(
				LocalDate.now().minusDays(2).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(1);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5),
				LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		MockHttpServletResponse response = mockMvc.perform(
				post("/v1/exposed").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC").content(json(exposeeRequest)))
				.andExpect(status().is(400)).andReturn().getResponse();
	}

	@Test
	public void cannotUseSameTokenTwice() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest
				.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(0);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));

		MockHttpServletResponse response = mockMvc.perform(
				post("/v1/exposed").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC").content(json(exposeeRequest)))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse();

		response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void canUseSameTokenTwiceIfFake() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest
				.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(1);
		String token = createToken(true, OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));

		MockHttpServletResponse response = mockMvc.perform(
				post("/v1/exposed").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC").content(json(exposeeRequest)))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse();

		response = mockMvc.perform(
				post("/v1/exposed").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC").content(json(exposeeRequest)))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse();

	}

	@Test
	public void cannotUseExpiredToken() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest
				.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(0);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusMinutes(5));

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
	}

	@Test
	public void cannotUseKeyDateInFuture() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest.setKeyDate(
				OffsetDateTime.now().plusDays(2).withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));

		exposeeRequest.setIsFake(0);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
	}

	@Test
	public void keyDateNotOlderThan21Days() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest.setKeyDate(
				OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusDays(22).toInstant().toEpochMilli());

		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(0);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5),
				"2020-01-01");

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
	}

	@Test
	public void cannotUseTokenWithWrongScope() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest
				.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(0);
		String token = createTokenWithScope(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5),
				"not-exposed");

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is(403)).andExpect(content().string("")).andReturn().getResponse();

		// Also for a 403 response, the token cannot be used a 2nd time
		response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void cannotUsedLongLivedToken() throws Exception {
		ExposeeRequest exposeeRequest = new ExposeeRequest();
		exposeeRequest.setAuthData(new ExposeeAuthData());
		exposeeRequest
				.setKeyDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
		exposeeRequest.setKey(Base64.getEncoder().encodeToString("testKey32Bytes--testKey32Bytes--".getBytes("UTF-8")));
		exposeeRequest.setIsFake(0);
		String token = createToken(OffsetDateTime.now(ZoneOffset.UTC).plusDays(90)); // very late expiration date
		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

}