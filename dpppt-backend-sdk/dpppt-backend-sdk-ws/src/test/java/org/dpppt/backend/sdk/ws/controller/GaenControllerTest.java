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
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.security.PublicKey;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dpppt.backend.sdk.model.ExposedKey;
import org.dpppt.backend.sdk.model.ExposeeAuthData;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.dpppt.backend.sdk.ws.util.GaenUnit;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.ExposeeRequestList;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

@SpringBootTest(properties = { "ws.app.jwt.publickey=classpath://generated_pub.pem" })
public class GaenControllerTest extends BaseControllerTest {
	@Autowired
	ProtoSignature signer;

	@Test
	public void testHello() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(get("/v1")).andExpect(status().is2xxSuccessful()).andReturn()
				.getResponse();

		assertNotNull(response);
		assertEquals("Hello from DP3T WS", response.getContentAsString());
	}

	@Test
	public void testMultipleKeyUpload() throws Exception {
		var requestList = new GaenRequest();
		var gaenKey1 = new GaenKey();
		gaenKey1.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey1.setRollingPeriod(144);
		gaenKey1.setFake(0);
		gaenKey1.setTransmissionRiskLevel(0);
		var gaenKey2 = new GaenKey();
		gaenKey2.setRollingStartNumber((int)Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey2.setRollingPeriod(144);
		gaenKey2.setFake(0);
		gaenKey2.setTransmissionRiskLevel(0);
		List<GaenKey> exposedKeys = new ArrayList<>();
		exposedKeys.add(gaenKey1);
		exposedKeys.add(gaenKey2);
		for(int i = 0; i < 12; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			exposedKeys.add(tmpKey);
		}
		requestList.setGaenKeys(exposedKeys);
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int)duration);
		gaenKey1.setFake(0);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
		response = mockMvc
				.perform(post("/v1/gaen/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void testMultipleKeyUploadFake() throws Exception {
		var requestList = new GaenRequest();
		var gaenKey1 = new GaenKey();
		gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey1.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey1.setRollingPeriod(144);
		gaenKey1.setTransmissionRiskLevel(0);
		var gaenKey2 = new GaenKey();
		gaenKey2.setRollingStartNumber((int)Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey2.setRollingPeriod(144);
		gaenKey2.setTransmissionRiskLevel(0);

		gaenKey1.setFake(1);
		gaenKey2.setFake(1);

		List<GaenKey> exposedKeys = new ArrayList<>();
		exposedKeys.add(gaenKey1);
		exposedKeys.add(gaenKey2);
		for(int i = 0; i < 12; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			exposedKeys.add(tmpKey);
		}
		requestList.setGaenKeys(exposedKeys);
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int)duration);
		
		String token = createToken(true, OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
		response = mockMvc
				.perform(post("/v1/gaen/exposedlist").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void testMultipleKeyUploadFakeAllKeysNeedToBeFake() throws Exception {
		var requestList = new GaenRequest();
		var gaenKey1 = new GaenKey();
		gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey1.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey1.setRollingPeriod(144);
		gaenKey1.setTransmissionRiskLevel(0);
		var gaenKey2 = new GaenKey();
		gaenKey2.setRollingStartNumber((int)Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey2.setRollingPeriod(144);
		gaenKey2.setTransmissionRiskLevel(0);

		gaenKey1.setFake(0);
		gaenKey2.setFake(1);

		List<GaenKey> exposedKeys = new ArrayList<>();
		exposedKeys.add(gaenKey1);
		exposedKeys.add(gaenKey2);
		for(int i = 0; i < 12; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			exposedKeys.add(tmpKey);
		}
		requestList.setGaenKeys(exposedKeys);
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int)duration);
		
		String token = createToken(true, OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
		response = mockMvc
				.perform(post("/v1/gaen/exposedlist").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}
	@Test
	public void testMultipleKeyUploadFakeIfJWTNotFakeAllKeysCanBeFake() throws Exception {
		var requestList = new GaenRequest();
		var gaenKey1 = new GaenKey();
		gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey1.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey1.setRollingPeriod(144);
		gaenKey1.setTransmissionRiskLevel(0);
		var gaenKey2 = new GaenKey();
		gaenKey2.setRollingStartNumber((int)Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey2.setRollingPeriod(144);
		gaenKey2.setTransmissionRiskLevel(0);

		gaenKey1.setFake(1);
		gaenKey2.setFake(1);

		List<GaenKey> exposedKeys = new ArrayList<>();
		exposedKeys.add(gaenKey1);
		exposedKeys.add(gaenKey2);
		for(int i = 0; i < 12; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			exposedKeys.add(tmpKey);
		}
		requestList.setGaenKeys(exposedKeys);
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int)duration);
		
		String token = createToken(false, OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is2xxSuccessful())
				.andReturn().getResponse();
		response = mockMvc
				.perform(post("/v1/gaen/exposedlist").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void testMultipleKeyNonEmptyUpload() throws Exception {
		var requestList = new GaenRequest();
		List<GaenKey> exposedKeys = new ArrayList<GaenKey>();
		requestList.setGaenKeys(exposedKeys);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is(400)).andReturn()
				.getResponse();
		response = mockMvc
				.perform(post("/v1/gaen/exposedlist").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void testMultipleKeyNonNullUpload() throws Exception {
		var requestList = new GaenRequest();
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int)duration);
		
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(requestList))).andExpect(status().is(400)).andReturn()
				.getResponse();
		response = mockMvc
				.perform(post("/v1/gaen/exposedlist").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + jwtToken).header("User-Agent", "MockMVC")
						.content(json(requestList)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void keyNeedsToBeBase64() throws Exception {
		GaenRequest exposeeRequest = new GaenRequest();
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int)duration);
		GaenKey key = new GaenKey();
		key.setKeyData("00000000testKey32Bytes--");
		key.setRollingPeriod(144);
		key.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for(int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			keys.add(tmpKey);
		}
		exposeeRequest.setGaenKeys(keys);
		
		String token = createToken(true, OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(
				post("/v1/gaen/exposed").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC").content(json(exposeeRequest)))
				.andExpect(status().is(400)).andReturn().getResponse();
	}

	@Test
	public void cannotUseKeyDateBeforeOnset() throws Exception {
		GaenRequest exposeeRequest = new GaenRequest();
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int)duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber((int)Duration.ofMillis(Instant.now().minus(Duration.ofDays(2)).toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for(int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			keys.add(tmpKey);
		}
		exposeeRequest.setGaenKeys(keys);

		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5),
				LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		MockHttpServletResponse response = mockMvc.perform(
				post("/v1/gaen/exposed").contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
						.header("User-Agent", "MockMVC").content(json(exposeeRequest)))
				.andExpect(status().is(400)).andReturn().getResponse();
	}

	@Test
	public void cannotUseExpiredToken() throws Exception {
		GaenRequest exposeeRequest = new GaenRequest();
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int)duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for(int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			keys.add(tmpKey);
		}
		exposeeRequest.setGaenKeys(keys);

		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusMinutes(5));

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/gaen/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
	}

	@Test
	public void cannotUseKeyDateInFuture() throws Exception {
		GaenRequest exposeeRequest = new GaenRequest();
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int)duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber((int)Duration.ofMillis(Instant.now().plus(Duration.ofDays(2)).toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for(int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			keys.add(tmpKey);
		}
		exposeeRequest.setGaenKeys(keys);
		
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/gaen/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
	}

	@Test
	public void keyDateNotOlderThan21Days() throws Exception {
		GaenRequest exposeeRequest = new GaenRequest();
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int)duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber((int)Duration.ofMillis(Instant.now().minus(Duration.ofDays(22)).toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for(int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			keys.add(tmpKey);
		}
		exposeeRequest.setGaenKeys(keys);

		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5),
				"2020-01-01");

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/gaen/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is4xxClientError()).andReturn().getResponse();
	}

	@Test
	public void cannotUseTokenWithWrongScope() throws Exception {
		GaenRequest exposeeRequest = new GaenRequest();
		var duration = Duration.ofMillis(LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli()).dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int)duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for(int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			keys.add(tmpKey);
		}
		exposeeRequest.setGaenKeys(keys);

		String token = createTokenWithScope(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5),
				"not-exposed");

		MockHttpServletResponse response = mockMvc
				.perform(post("/v1/gaen/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is(403)).andExpect(content().string("")).andReturn().getResponse();

		// Also for a 403 response, the token cannot be used a 2nd time
		response = mockMvc
				.perform(post("/v1/gaen/exposed").contentType(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token).header("User-Agent", "MockMVC")
						.content(json(exposeeRequest)))
				.andExpect(status().is(401)).andExpect(content().string("")).andReturn().getResponse();
	}

	@Test
	public void zipContainsFiles() throws Exception {
		insertNKeysPerDayInInterval(14, OffsetDateTime.now(ZoneOffset.UTC).minusDays(15), OffsetDateTime.now(ZoneOffset.UTC));
		MockHttpServletResponse response = mockMvc.perform(get("/v1/gaen/exposed/" + LocalDate.now().minusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli())
						.header("User-Agent", "MockMVC")).andExpect(status().is2xxSuccessful())
						.andReturn().getResponse();
		ByteArrayInputStream bais = new ByteArrayInputStream(response.getContentAsByteArray());
		ZipInputStream zip = new ZipInputStream(bais);

		ZipEntry binary = zip.getNextEntry();
		assertEquals(binary.getName(), "export.bin");
		ByteArrayDataOutput bado = ByteStreams.newDataOutput();
		zip.readNBytes(16);
		while(zip.available() > 0) {
			bado.write(zip.readNBytes(1000));
		}
		assertEquals(zip.available(), 0);
		byte[] binProto = bado.toByteArray();


		ByteArrayDataOutput bado2 = ByteStreams.newDataOutput();
		ZipEntry signature = zip.getNextEntry();
		assertEquals(signature.getName(), "export.sig");
		while(zip.available() > 0){
			bado2.write(zip.readNBytes(1000));
		}
		assertEquals(zip.available(), 0);
		byte[] sigProto = bado2.toByteArray();

		assertNull(zip.getNextEntry());
		var list = TemporaryExposureKeyFormat.TEKSignatureList.parseFrom(sigProto);
		var export =TemporaryExposureKeyFormat.TemporaryExposureKeyExport.parseFrom(binProto);
		var sig = list.getSignatures(0);
		java.security.Signature signatureVerifier = java.security.Signature.getInstance(sig.getSignatureInfo().getSignatureAlgorithm().trim());
		signatureVerifier.initVerify(signer.getPublicKey());
		signatureVerifier.update(binProto);
		assertTrue(signatureVerifier.verify(sig.getSignature().toByteArray()));

		zip.close();
		bais.close();
	}

	private void insertNKeysPerDayInInterval(int N, OffsetDateTime start, OffsetDateTime end) throws Exception{
		var current = start;
		while (current.isBefore(end)) {
			GaenRequest exposeeRequest = new GaenRequest();
			List<GaenKey> keys = new ArrayList<>();
			int lastRolling = (int)Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10));
			for(int i =0; i < N; i++) {
				GaenKey key = new GaenKey();
				key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
				key.setRollingPeriod(144);
				key.setRollingStartNumber(lastRolling);
				key.setTransmissionRiskLevel(1);
				key.setFake(0);
				keys.add(key);
			}
			exposeeRequest.setGaenKeys(keys);
			var duration = Duration.of(lastRolling, GaenUnit.TenMinutes).plusDays(1).dividedBy(Duration.ofMinutes(10));
			exposeeRequest.setDelayedKeyDate((int)duration);
			String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
			MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
					.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
					.header("User-Agent", "MockMVC").content(json(exposeeRequest))).andExpect(status().is2xxSuccessful())
					.andReturn().getResponse();
			current = current.plusDays(1);
		}
	}

}