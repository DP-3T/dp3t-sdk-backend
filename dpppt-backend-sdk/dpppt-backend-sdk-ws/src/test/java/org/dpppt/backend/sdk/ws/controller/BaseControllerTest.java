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

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.servlet.Filter;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.filter.ResponseWrapperFilter;
import org.dpppt.backend.sdk.ws.util.TestJDBCGaen;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "dev", "jwt", "debug", "test-config" })
@TestPropertySource(properties = { "ws.app.source=org.dpppt.demo" })
public abstract class BaseControllerTest {

	protected MockMvc mockMvc;

	protected final String jwtToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1LVJxcVRUWW9tZnBWejA2VlJFT2ZyYmNxYTVXdTJkX1g4MnZfWlRNLVZVIn0.eyJqdGkiOiI4ZmE5MWRlMi03YmYwLTRhNmYtOWIzZC1hNzdiZDM3ZDdiMTMiLCJleHAiOjE1ODczMTYzMTgsIm5iZiI6MCwiaWF0IjoxNTg3MzE2MDE4LCJpc3MiOiJodHRwczovL2lkZW50aXR5LXIuYml0LmFkbWluLmNoL3JlYWxtcy9iYWctcHRzIiwic3ViIjoiMWVmYTliZWYtOWU5ZC00MjNjLTkxMjctZmQwYjAxNWQxOTY2IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoicHRhLWFwcC1iYWNrZW5kIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGVjYzQ3Y2YtMmUyZi00NjZlLThiNTAtZmQ4MTAzZmQ4ZDNhIiwiYWNyIjoiMSIsInNjb3BlIjoiZXhwb3NlZCIsIm9uc2V0IjoiMjAyMC0wNS0yNSJ9.J5beGE6GjgRWEZfwzB9_G6X1uTZcZdm7Mkng8od5Fr3UPT4BbkKgPbpGRscouiAPBjOlZDCs3rcT_qiioX5wAZ0UjqLTe370K53vb1I_f4nQKfTMBYfzvdpS5i5V64LoKbXHpF7PLsGSiox6dA8g5Ssqf5uoTHz1_NY-6GvVq-LmFozV6_1zzYkBVZCLVh0gsqcG9EH2peuhEt9akv_Jmc1Ls0lZQeU1szeRmsk44mg8_FbG33yB3F0azhs0pfEuuYCzGbAqdFCU2RDnRCOXXr7o8Z_klrKE6NArWgbHbk8CE0a-3UwEdi6zw0xm1VNwbnMtjxVcyxECw7V2bSNu9A";

	@Autowired
	private WebApplicationContext webApplicationContext;
	protected ObjectMapper objectMapper;

	@Autowired
	private Filter springSecurityFilterChain;

	@Autowired
	private ResponseWrapperFilter filter;

	@Autowired
	DataSource dataSource;

	protected TestJDBCGaen testGaenDataService;

	@Before
	public void setup() throws Exception {
		loadPrivateKey();
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilter(springSecurityFilterChain).addFilter(filter, "/*").build();
		this.objectMapper = new ObjectMapper(new JsonFactory());
		this.objectMapper.registerModule(new JavaTimeModule());
		this.testGaenDataService = new TestJDBCGaen("hsqldb", dataSource);
	}

	private void loadPrivateKey() throws Exception {
		InputStream inputStream = new ClassPathResource("generated_private.pem").getInputStream();
		String key = IOUtils.toString(inputStream);
		PKCS8EncodedKeySpec keySpecX509 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		privateKey = (PrivateKey) kf.generatePrivate(keySpecX509);
	}

	protected String json(Object o) throws IOException {
		return objectMapper.writeValueAsString(o);
	}

	protected PublicKey publicKey;
	protected PrivateKey privateKey;

	protected String createToken(UTCInstant expiresAt) {
		Claims claims = Jwts.claims();
		claims.put("scope", "exposed");
		claims.put("onset", "2020-04-20");
		claims.put("fake", "0");
		return Jwts.builder().setClaims(claims).setId(UUID.randomUUID().toString())
				.setSubject("test-subject" + OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString()).setExpiration(expiresAt.getDate())
				.setIssuedAt(UTCInstant.now().getDate()).signWith((Key) privateKey).compact();
	}
	protected String createToken(UTCInstant expiresAt, String onset) {
		Claims claims = Jwts.claims();
		claims.put("scope", "exposed");
		claims.put("fake", "0");
		claims.put("onset", onset);
		return Jwts.builder().setClaims(claims).setId(UUID.randomUUID().toString())
				.setSubject("test-subject" + OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString()).setExpiration(expiresAt.getDate())
				.setIssuedAt(UTCInstant.now().getDate()).signWith((Key) privateKey).compact();
	}

	protected String createToken(String subject, UTCInstant expiresAt) {
		Claims claims = Jwts.claims();
		claims.put("scope", "exposed");
		claims.put("fake", "0");
		return Jwts.builder().setSubject(subject).setExpiration(expiresAt.getDate()).setClaims(claims)
				.setId(UUID.randomUUID().toString()).signWith((Key) privateKey).compact();
	}

	protected String createToken(boolean fake, UTCInstant expiresAt) {
		Claims claims = Jwts.claims();
		claims.put("scope", "exposed");
		claims.put("onset", "2020-04-20");
		claims.put("fake", fake? "1": "0");
		return Jwts.builder().setClaims(claims).setId(UUID.randomUUID().toString())
				.setSubject("test-subject" + UTCInstant.now().getOffsetDateTime().toString())
				.setExpiration(expiresAt.getDate())
				.setIssuedAt(UTCInstant.now().getDate()).signWith((Key) privateKey).compact();
	}

	protected String createTokenWithScope(UTCInstant expiresAt, String scope) {
		Claims claims = Jwts.claims();
		claims.put("scope", scope);
		claims.put("fake", "0");
		claims.put("onset", "2020-04-20");
		return Jwts.builder().setClaims(claims).setId(UUID.randomUUID().toString())
				.setSubject("test-subject" + OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString()).setExpiration(expiresAt.getDate())
				.setIssuedAt(UTCInstant.now().getDate()).signWith((Key) privateKey).compact();
	}
	protected String createMaliciousToken(UTCInstant expiresAt) {
		Claims claims = Jwts.claims();
		claims.put("scope", "exposed");
		claims.put("onset", "2020-04-20");
		claims.put("fake", "0");
		return Jwts.builder().setClaims(claims).setId(UUID.randomUUID().toString())
				.setSubject("test-subject" + OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString()).setExpiration(Date.from(expiresAt.toInstant()))
				.setIssuedAt(Date.from(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant())).compact();
	}

}
