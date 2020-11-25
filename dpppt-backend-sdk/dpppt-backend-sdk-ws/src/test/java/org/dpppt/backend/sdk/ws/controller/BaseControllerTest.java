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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.Filter;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat.TEKSignatureList;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat.TemporaryExposureKeyExport;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.filter.ResponseWrapperFilter;
import org.dpppt.backend.sdk.ws.security.KeyVault;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.dpppt.backend.sdk.ws.util.TestJDBCGaen;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "jwt", "debug", "test-config"})
@TestPropertySource(properties = {"ws.app.source=org.dpppt.demo"})
public abstract class BaseControllerTest {

  protected MockMvc mockMvc;

  protected final String jwtToken =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1LVJxcVRUWW9tZnBWejA2VlJFT2ZyYmNxYTVXdTJkX1g4MnZfWlRNLVZVIn0.eyJqdGkiOiI4ZmE5MWRlMi03YmYwLTRhNmYtOWIzZC1hNzdiZDM3ZDdiMTMiLCJleHAiOjE1ODczMTYzMTgsIm5iZiI6MCwiaWF0IjoxNTg3MzE2MDE4LCJpc3MiOiJodHRwczovL2lkZW50aXR5LXIuYml0LmFkbWluLmNoL3JlYWxtcy9iYWctcHRzIiwic3ViIjoiMWVmYTliZWYtOWU5ZC00MjNjLTkxMjctZmQwYjAxNWQxOTY2IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoicHRhLWFwcC1iYWNrZW5kIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZGVjYzQ3Y2YtMmUyZi00NjZlLThiNTAtZmQ4MTAzZmQ4ZDNhIiwiYWNyIjoiMSIsInNjb3BlIjoiZXhwb3NlZCIsIm9uc2V0IjoiMjAyMC0wNS0yNSJ9.J5beGE6GjgRWEZfwzB9_G6X1uTZcZdm7Mkng8od5Fr3UPT4BbkKgPbpGRscouiAPBjOlZDCs3rcT_qiioX5wAZ0UjqLTe370K53vb1I_f4nQKfTMBYfzvdpS5i5V64LoKbXHpF7PLsGSiox6dA8g5Ssqf5uoTHz1_NY-6GvVq-LmFozV6_1zzYkBVZCLVh0gsqcG9EH2peuhEt9akv_Jmc1Ls0lZQeU1szeRmsk44mg8_FbG33yB3F0azhs0pfEuuYCzGbAqdFCU2RDnRCOXXr7o8Z_klrKE6NArWgbHbk8CE0a-3UwEdi6zw0xm1VNwbnMtjxVcyxECw7V2bSNu9A";

  @Autowired private WebApplicationContext webApplicationContext;
  protected ObjectMapper objectMapper;

  @Autowired private Filter springSecurityFilterChain;

  @Autowired private ResponseWrapperFilter filter;

  @Autowired DataSource dataSource;

  @Autowired ProtoSignature signer;
  @Autowired KeyVault keyVault;
  @Autowired GAENDataService gaenDataService;

  protected TestJDBCGaen testGaenDataService;

  protected static final String androidUserAgent =
      "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29";

  protected Duration releaseBucketDuration = Duration.ofMillis(7200000L);

  @Before
  public void setup() throws Exception {
    loadPrivateKey();
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilter(springSecurityFilterChain)
            .addFilter(filter, "/*")
            .build();
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
    return Jwts.builder()
        .setClaims(claims)
        .setId(UUID.randomUUID().toString())
        .setSubject(
            "test-subject" + OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString())
        .setExpiration(expiresAt.getDate())
        .setIssuedAt(UTCInstant.now().getDate())
        .signWith((Key) privateKey)
        .compact();
  }

  protected String createToken(UTCInstant expiresAt, String onset) {
    Claims claims = Jwts.claims();
    claims.put("scope", "exposed");
    claims.put("fake", "0");
    claims.put("onset", onset);
    return Jwts.builder()
        .setClaims(claims)
        .setId(UUID.randomUUID().toString())
        .setSubject(
            "test-subject" + OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString())
        .setExpiration(expiresAt.getDate())
        .setIssuedAt(UTCInstant.now().getDate())
        .signWith((Key) privateKey)
        .compact();
  }

  protected String createToken(String subject, UTCInstant expiresAt) {
    Claims claims = Jwts.claims();
    claims.put("scope", "exposed");
    claims.put("fake", "0");
    return Jwts.builder()
        .setSubject(subject)
        .setExpiration(expiresAt.getDate())
        .setClaims(claims)
        .setId(UUID.randomUUID().toString())
        .signWith((Key) privateKey)
        .compact();
  }

  protected String createToken(boolean fake, UTCInstant expiresAt) {
    Claims claims = Jwts.claims();
    claims.put("scope", "exposed");
    claims.put("onset", "2020-04-20");
    claims.put("fake", fake ? "1" : "0");
    return Jwts.builder()
        .setClaims(claims)
        .setId(UUID.randomUUID().toString())
        .setSubject("test-subject" + UTCInstant.now().getOffsetDateTime().toString())
        .setExpiration(expiresAt.getDate())
        .setIssuedAt(UTCInstant.now().getDate())
        .signWith((Key) privateKey)
        .compact();
  }

  protected String createTokenWithScope(UTCInstant expiresAt, String scope) {
    Claims claims = Jwts.claims();
    claims.put("scope", scope);
    claims.put("fake", "0");
    claims.put("onset", "2020-04-20");
    return Jwts.builder()
        .setClaims(claims)
        .setId(UUID.randomUUID().toString())
        .setSubject(
            "test-subject" + OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString())
        .setExpiration(expiresAt.getDate())
        .setIssuedAt(UTCInstant.now().getDate())
        .signWith((Key) privateKey)
        .compact();
  }

  protected String createMaliciousToken(UTCInstant expiresAt) {
    Claims claims = Jwts.claims();
    claims.put("scope", "exposed");
    claims.put("onset", "2020-04-20");
    claims.put("fake", "0");
    return Jwts.builder()
        .setClaims(claims)
        .setId(UUID.randomUUID().toString())
        .setSubject(
            "test-subject" + OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString())
        .setExpiration(expiresAt.getDate())
        .setIssuedAt(UTCInstant.now().getDate())
        .compact();
  }

  protected void testNKeys(UTCInstant now, int n, boolean shouldSucceed) throws Exception {
    var requestList = new GaenRequest();
    var gaenKey1 = new GaenKey();
    gaenKey1.setRollingStartNumber((int) now.atStartOfDay().minusDays(1).get10MinutesSince1970());
    gaenKey1.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytesa1".getBytes("UTF-8")));
    gaenKey1.setRollingPeriod(144);
    gaenKey1.setFake(0);
    gaenKey1.setTransmissionRiskLevel(0);
    var gaenKey2 = new GaenKey();
    gaenKey2.setRollingStartNumber((int) now.atStartOfDay().minusDays(1).get10MinutesSince1970());
    gaenKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytesb2".getBytes("UTF-8")));
    gaenKey2.setRollingPeriod(144);
    gaenKey2.setFake(0);
    gaenKey2.setTransmissionRiskLevel(0);
    var gaenKey3 = new GaenKey();
    gaenKey3.setRollingStartNumber((int) now.atStartOfDay().get10MinutesSince1970());
    gaenKey3.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytesc3".getBytes("UTF-8")));
    gaenKey3.setRollingPeriod(144);
    gaenKey3.setFake(0);
    gaenKey3.setTransmissionRiskLevel(0);
    List<GaenKey> exposedKeys = new ArrayList<>();
    exposedKeys.add(gaenKey1);
    exposedKeys.add(gaenKey2);
    exposedKeys.add(gaenKey3);
    for (int i = 0; i < n - 3; i++) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber((int) now.atStartOfDay().get10MinutesSince1970());
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytesaa".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(144);
      tmpKey.setFake(1);
      tmpKey.setTransmissionRiskLevel(0);
      exposedKeys.add(tmpKey);
    }
    requestList.setGaenKeys(exposedKeys);
    var duration = now.atStartOfDay().plusDays(1).get10MinutesSince1970();
    requestList.setDelayedKeyDate((int) duration);
    gaenKey1.setFake(0);
    String token = createToken(now.plusMinutes(5));
    var requestBuilder =
        mockMvc.perform(
            post("/v1/gaen/exposed")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", androidUserAgent)
                .content(json(requestList)));
    MvcResult response;

    if (shouldSucceed) {
      response = requestBuilder.andExpect(request().asyncStarted()).andReturn();
      mockMvc.perform(asyncDispatch(response)).andExpect(status().is2xxSuccessful());
    } else {
      response = requestBuilder.andExpect(status().is(400)).andReturn();
      return;
    }
    response =
        mockMvc
            .perform(
                post("/v1/gaen/exposed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + jwtToken)
                    .header("User-Agent", androidUserAgent)
                    .content(json(requestList)))
            .andExpect(status().is(401))
            .andExpect(request().asyncNotStarted())
            .andExpect(content().string(""))
            .andReturn();

    var result =
        gaenDataService.getSortedExposedForKeyDate(
            now.atStartOfDay().minusDays(1),
            UTCInstant.midnight1970(),
            now.roundToNextBucket(releaseBucketDuration),
            now);
    assertEquals(2, result.size());
    for (var key : result) {
      assertEquals(Integer.valueOf(144), key.getRollingPeriod());
    }

    result =
        gaenDataService.getSortedExposedForKeyDate(
            now.atStartOfDay().minusDays(1),
            UTCInstant.midnight1970(),
            now.roundToBucketStart(releaseBucketDuration),
            now);
    assertEquals(0, result.size());

    // third key should be released tomorrow (at four)
    var tomorrow2AM = now.atStartOfDay().plusDays(1).plusHours(4).plusSeconds(1);
    result =
        gaenDataService.getSortedExposedForKeyDate(
            now.atStartOfDay(),
            UTCInstant.midnight1970(),
            tomorrow2AM.roundToNextBucket(releaseBucketDuration),
            tomorrow2AM);
    assertEquals(1, result.size());

    result =
        gaenDataService.getSortedExposedForKeyDate(
            now.atStartOfDay(),
            UTCInstant.midnight1970(),
            now.roundToNextBucket(releaseBucketDuration),
            now);
    assertEquals(0, result.size());

    result =
        gaenDataService.getSortedExposedForKeyDate(
            now.atStartOfDay(), UTCInstant.midnight1970(), now.atStartOfDay().plusDays(1), now);
    assertEquals(0, result.size());
  }

  /** Verifies a zip in zip response, that each inner zip is again valid. */
  protected void verifyZipInZipResponse(
      MockHttpServletResponse response, int expectKeyCount, int expectedRollingPeriod)
      throws Exception {
    ByteArrayInputStream baisOuter = new ByteArrayInputStream(response.getContentAsByteArray());
    ZipInputStream zipOuter = new ZipInputStream(baisOuter);
    ZipEntry entry = zipOuter.getNextEntry();
    while (entry != null) {
      ZipInputStream zipInner =
          new ZipInputStream(new ByteArrayInputStream(zipOuter.readAllBytes()));
      verifyKeyZip(zipInner, expectKeyCount, expectedRollingPeriod);
      entry = zipOuter.getNextEntry();
    }
  }

  /**
   * Fetches the keys in a zip file returned from a `/v1/gaen/exposed` response.
   *
   * @param response holding a zip file with the keys
   * @return the keys in the zip file
   * @throws IOException
   */
  protected TemporaryExposureKeyExport getZipKeys(MockHttpServletResponse response)
      throws IOException {
    ByteArrayInputStream baisZip = new ByteArrayInputStream(response.getContentAsByteArray());
    ZipInputStream keyZipInputstream = new ZipInputStream(baisZip);
    ZipEntry entry = keyZipInputstream.getNextEntry();

    byte[] exportBin;
    byte[] keyProto = null;

    while (entry != null) {
      if (entry.getName().equals("export.bin")) {
        exportBin = keyZipInputstream.readAllBytes();
        keyProto = new byte[exportBin.length - 16];
        System.arraycopy(exportBin, 16, keyProto, 0, keyProto.length);
      }
      entry = keyZipInputstream.getNextEntry();
    }

    assertNotNull(keyProto);
    return TemporaryExposureKeyExport.parseFrom(keyProto);
  }

  /** Verifies a zip response, checks if keys and signature is correct. */
  protected void verifyZipResponse(
      MockHttpServletResponse response, int expectKeyCount, int expectedRollingPeriod)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    ByteArrayInputStream baisZip = new ByteArrayInputStream(response.getContentAsByteArray());
    ZipInputStream keyZipInputstream = new ZipInputStream(baisZip);
    verifyKeyZip(keyZipInputstream, expectKeyCount, expectedRollingPeriod);
  }

  protected void verifyKeyZip(
      ZipInputStream keyZipInputstream, int expectKeyCount, int expectedRollingPeriod)
      throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    ZipEntry entry = keyZipInputstream.getNextEntry();
    boolean foundData = false;
    boolean foundSignature = false;

    byte[] signatureProto = null;
    byte[] exportBin = null;
    byte[] keyProto = null;

    while (entry != null) {
      if (entry.getName().equals("export.bin")) {
        foundData = true;
        exportBin = keyZipInputstream.readAllBytes();
        keyProto = new byte[exportBin.length - 16];
        System.arraycopy(exportBin, 16, keyProto, 0, keyProto.length);
      }
      if (entry.getName().equals("export.sig")) {
        foundSignature = true;
        signatureProto = keyZipInputstream.readAllBytes();
      }
      entry = keyZipInputstream.getNextEntry();
    }

    assertTrue(foundData, "export.bin not found in zip");
    assertTrue(foundSignature, "export.sig not found in zip");

    TEKSignatureList list = TEKSignatureList.parseFrom(signatureProto);
    TemporaryExposureKeyExport export = TemporaryExposureKeyExport.parseFrom(keyProto);
    for (var key : export.getKeysList()) {
      assertEquals(expectedRollingPeriod, key.getRollingPeriod());
    }
    var sig = list.getSignatures(0);
    java.security.Signature signatureVerifier =
        java.security.Signature.getInstance(sig.getSignatureInfo().getSignatureAlgorithm().trim());
    signatureVerifier.initVerify(signer.getPublicKey());

    signatureVerifier.update(exportBin);
    assertTrue(
        signatureVerifier.verify(sig.getSignature().toByteArray()),
        "Could not verify signature in zip file");
    assertEquals(expectKeyCount, export.getKeysCount());
  }
}
