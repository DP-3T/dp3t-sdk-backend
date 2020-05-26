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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.model.gaen.GaenSecondDay;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat;
import org.dpppt.backend.sdk.ws.security.KeyVault;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;

@SpringBootTest(properties = { "ws.app.jwt.publickey=classpath://generated_pub.pem",
		"logging.level.org.springframework.security=DEBUG", "ws.exposedlist.batchlength=7200000" })
public class GaenControllerTest extends BaseControllerTest {
	@Autowired
	ProtoSignature signer;
	@Autowired
	KeyVault keyVault;
	@Autowired
	GAENDataService gaenDataService;

	private static final Logger logger = LoggerFactory.getLogger(GaenControllerTest.class);

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
		gaenKey1.setRollingStartNumber(
				(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey1.setRollingPeriod(144);
		gaenKey1.setFake(0);
		gaenKey1.setTransmissionRiskLevel(0);
		var gaenKey2 = new GaenKey();
		gaenKey2.setRollingStartNumber((int) Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10)));
		gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey2.setRollingPeriod(144);
		gaenKey2.setFake(0);
		gaenKey2.setTransmissionRiskLevel(0);
		List<GaenKey> exposedKeys = new ArrayList<>();
		exposedKeys.add(gaenKey1);
		exposedKeys.add(gaenKey2);
		for (int i = 0; i < 12; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			exposedKeys.add(tmpKey);
		}
		requestList.setGaenKeys(exposedKeys);
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int) duration);
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
		gaenKey1.setRollingStartNumber(
				(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey1.setRollingPeriod(144);
		gaenKey1.setTransmissionRiskLevel(0);
		var gaenKey2 = new GaenKey();
		gaenKey2.setRollingStartNumber((int) Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10)));
		gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey2.setRollingPeriod(144);
		gaenKey2.setTransmissionRiskLevel(0);

		gaenKey1.setFake(1);
		gaenKey2.setFake(1);

		List<GaenKey> exposedKeys = new ArrayList<>();
		exposedKeys.add(gaenKey1);
		exposedKeys.add(gaenKey2);
		for (int i = 0; i < 12; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			exposedKeys.add(tmpKey);
		}
		requestList.setGaenKeys(exposedKeys);
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int) duration);

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
		gaenKey1.setRollingStartNumber(
				(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey1.setRollingPeriod(144);
		gaenKey1.setTransmissionRiskLevel(0);
		var gaenKey2 = new GaenKey();
		gaenKey2.setRollingStartNumber((int) Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10)));
		gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey2.setRollingPeriod(144);
		gaenKey2.setTransmissionRiskLevel(0);

		gaenKey1.setFake(0);
		gaenKey2.setFake(1);

		List<GaenKey> exposedKeys = new ArrayList<>();
		exposedKeys.add(gaenKey1);
		exposedKeys.add(gaenKey2);
		for (int i = 0; i < 12; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			exposedKeys.add(tmpKey);
		}
		requestList.setGaenKeys(exposedKeys);
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int) duration);

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
		gaenKey1.setRollingStartNumber(
				(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		gaenKey1.setRollingPeriod(144);
		gaenKey1.setTransmissionRiskLevel(0);
		var gaenKey2 = new GaenKey();
		gaenKey2.setRollingStartNumber((int) Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10)));
		gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		gaenKey2.setRollingPeriod(144);
		gaenKey2.setTransmissionRiskLevel(0);

		gaenKey1.setFake(1);
		gaenKey2.setFake(1);

		List<GaenKey> exposedKeys = new ArrayList<>();
		exposedKeys.add(gaenKey1);
		exposedKeys.add(gaenKey2);
		for (int i = 0; i < 12; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			exposedKeys.add(tmpKey);
		}
		requestList.setGaenKeys(exposedKeys);
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int) duration);

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
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		requestList.setDelayedKeyDate((int) duration);

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
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int) duration);
		GaenKey key = new GaenKey();
		key.setKeyData("00000000testKey32Bytes--");
		key.setRollingPeriod(144);
		key.setRollingStartNumber(
				(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for (int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			keys.add(tmpKey);
		}
		exposeeRequest.setGaenKeys(keys);

		String token = createToken(true, OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(exposeeRequest))).andExpect(status().is(400)).andReturn()
				.getResponse();
	}

	@Test
	public void cannotUseKeyDateBeforeOnset() throws Exception {
		GaenRequest exposeeRequest = new GaenRequest();
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int) duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber((int) Duration.ofMillis(Instant.now().minus(Duration.ofDays(2)).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for (int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(1);
			tmpKey.setTransmissionRiskLevel(0);
			keys.add(tmpKey);
		}
		exposeeRequest.setGaenKeys(keys);

		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5),
				LocalDate.now().format(DateTimeFormatter.ISO_DATE));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(exposeeRequest))).andExpect(status().is(400)).andReturn()
				.getResponse();
	}

	@Test
	public void cannotUseExpiredToken() throws Exception {
		GaenRequest exposeeRequest = new GaenRequest();
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int) duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber(
				(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for (int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
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
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int) duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber((int) Duration.ofMillis(Instant.now().plus(Duration.ofDays(2)).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for (int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
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
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int) duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber((int) Duration.ofMillis(Instant.now().minus(Duration.ofDays(22)).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for (int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
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
		var duration = Duration.ofMillis(
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli())
				.dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate((int) duration);
		GaenKey key = new GaenKey();
		key.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		key.setRollingPeriod(144);
		key.setRollingStartNumber(
				(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
		key.setTransmissionRiskLevel(1);
		key.setFake(1);
		List<GaenKey> keys = new ArrayList<>();
		keys.add(key);
		for (int i = 0; i < 13; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber(
					(int) Duration.ofMillis(Instant.now().toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
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
	public void uploadKeysAndUploadKeyNextDay() throws Exception {
		GaenRequest exposeeRequest = new GaenRequest();
		List<GaenKey> keys = new ArrayList<>();
		for (int i = 0; i < 14; i++) {
			var tmpKey = new GaenKey();
			tmpKey.setRollingStartNumber((int) Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
					.dividedBy(Duration.ofMinutes(10)));
			tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
			tmpKey.setRollingPeriod(144);
			tmpKey.setFake(0);
			tmpKey.setTransmissionRiskLevel(0);
			keys.add(tmpKey);
		}
		var delayedKeyDateSent = (int) Duration.ofSeconds(LocalDate.now().atStartOfDay(ZoneOffset.UTC).toEpochSecond())
				.dividedBy(Duration.ofMinutes(10));
		exposeeRequest.setDelayedKeyDate(delayedKeyDateSent);
		exposeeRequest.setGaenKeys(keys);
		String token = createToken(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).plusMinutes(5));
		MockHttpServletResponse response = mockMvc.perform(post("/v1/gaen/exposed")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
				.header("User-Agent", "MockMVC").content(json(exposeeRequest))).andExpect(status().is(200)).andReturn()
				.getResponse();
		assertTrue(response.containsHeader("Authorization"), "Authorization header not found in response");
		String jwtString = response.getHeader("Authorization").replace("Bearer ", "");
		Jwt jwtToken = Jwts.parserBuilder().setSigningKey(keyVault.get("nextDayJWT").getPublic()).build()
				.parse(jwtString);
		GaenSecondDay secondDay = new GaenSecondDay();
		var tmpKey = new GaenKey();
		tmpKey.setRollingStartNumber(delayedKeyDateSent);
		tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
		tmpKey.setRollingPeriod(144);
		tmpKey.setFake(0);
		tmpKey.setTransmissionRiskLevel(0);
		secondDay.setDelayedKey(tmpKey);
		response = mockMvc.perform(post("/v1/gaen/exposednextday").contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + jwtString).header("User-Agent", "MockMVC")
				.content(json(secondDay))).andExpect(status().is(200)).andReturn().getResponse();
	}

	@Ignore
	@Test
	public void zipContainsFiles() throws Exception {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		// insert two times 5 keys per day for the last 14 days. the second batch has a
		// different received at timestamp. (+6 hours)
		insertNKeysPerDayInInterval(14,
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC).minusDays(4),
				OffsetDateTime.now(ZoneOffset.UTC), now.minus(Duration.ofDays(1)).atOffset(ZoneOffset.UTC));

		insertNKeysPerDayInInterval(14,
				LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC).minusDays(4),
				OffsetDateTime.now(ZoneOffset.UTC), now.minus(Duration.ofHours(12)).atOffset(ZoneOffset.UTC));

		// request the keys with date date 1 day ago. no publish until.
		MockHttpServletResponse response = mockMvc
				.perform(get("/v1/gaen/exposed/"
						+ now.minusDays(8).toLocalDate().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
								.header("User-Agent", "MockMVC"))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse();

		Long publishedUntil = Long.parseLong(response.getHeader("X-PUBLISHED-UNTIL"));
		assertTrue(publishedUntil < System.currentTimeMillis(), "Published until must be in the past");

		verifyZipResponse(response, 10);

		// request again the keys with date date 1 day ago. with publish until, so that
		// we only get the second batch.
		MockHttpServletResponse responseWithPublishedAfter = mockMvc
				.perform(get("/v1/gaen/exposed/"
						+ now.minusDays(8).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli())
								.header("User-Agent", "MockMVC").param("publishedafter",
										Long.toString(now.minusDays(1).toLocalDate().atStartOfDay(ZoneId.of("UTC"))
												.plus(Duration.ofHours(2)).toInstant().toEpochMilli())))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse();

		verifyZipResponse(responseWithPublishedAfter, 5);
	}

	private void verifyZipResponse(MockHttpServletResponse response, int expectKeyCount)
			throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		ByteArrayInputStream baisOuter = new ByteArrayInputStream(response.getContentAsByteArray());
		ZipInputStream zipOuter = new ZipInputStream(baisOuter);
		ZipEntry entry = zipOuter.getNextEntry();
		boolean foundData = false;
		boolean foundSignature = false;

		byte[] signatureProto = null;
		byte[] exportBin = null;
		byte[] keyProto = null;

		while (entry != null) {
			if (entry.getName().equals("export.bin")) {
				foundData = true;
				exportBin = zipOuter.readAllBytes();
				keyProto = new byte[exportBin.length-16];
				System.arraycopy(exportBin, 16, keyProto, 0, keyProto.length);
			}
			if (entry.getName().equals("export.sig")) {
				foundSignature = true;
				signatureProto = zipOuter.readAllBytes();
			}
			entry = zipOuter.getNextEntry();
		}

		assertTrue(foundData, "export.bin not found in zip");
		assertTrue(foundSignature, "export.sig not found in zip");

		var list = TemporaryExposureKeyFormat.TEKSignatureList.parseFrom(signatureProto);
		var export = TemporaryExposureKeyFormat.TemporaryExposureKeyExport.parseFrom(keyProto);
		var sig = list.getSignatures(0);
		java.security.Signature signatureVerifier = java.security.Signature
				.getInstance(sig.getSignatureInfo().getSignatureAlgorithm().trim());
		signatureVerifier.initVerify(signer.getPublicKey());

		signatureVerifier.update(exportBin);
		assertTrue(signatureVerifier.verify(sig.getSignature().toByteArray()),
				"Could not verify signature in zip file");
		assertEquals(expectKeyCount, export.getKeysCount());
	}

	private void insertNKeysPerDayInInterval(int n, OffsetDateTime start, OffsetDateTime end, OffsetDateTime receivedAt)
			throws Exception {
		var current = start;
		Map<Integer, Integer> rollingToCount = new HashMap<>();
		while (current.isBefore(end)) {
			List<GaenKey> keys = new ArrayList<>();
			SecureRandom random = new SecureRandom();
			int lastRolling = (int) Duration.ofMillis(start.toInstant().toEpochMilli())
					.dividedBy(Duration.ofMinutes(10));
			for (int i = 0; i < n; i++) {
				GaenKey key = new GaenKey();
				byte[] keyBytes = new byte[16];
				random.nextBytes(keyBytes);
				key.setKeyData(Base64.getEncoder().encodeToString(keyBytes));
				key.setRollingPeriod(144);
				logger.info("Rolling Start number: " + lastRolling);
				key.setRollingStartNumber(lastRolling);
				key.setTransmissionRiskLevel(1);
				key.setFake(0);
				keys.add(key);
				
				Integer count = rollingToCount.get(lastRolling);
				if (count == null) {
					count = 0;
				}
				count = count + 1;
				rollingToCount.put(lastRolling, count);
				
				lastRolling -= Duration.ofDays(1).dividedBy(Duration.ofMinutes(10));
				
			}
			testGaenDataService.upsertExposees(keys, receivedAt);
			current = current.plusDays(1);
		}
		for (Entry<Integer, Integer> entry: rollingToCount.entrySet()) {
			logger.info("Rolling start number: " + entry.getKey() + " -> count: " + entry.getValue());
		}
	}

}