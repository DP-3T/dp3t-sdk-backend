/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.security.signature;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author bachmann
 * created on 24.04.20
 **/
public class SignatureResponseWrapperTest {


    private MockHttpServletResponse response;

    @Test
    public void testSignaturResponseWrapper() throws IOException, NoSuchAlgorithmException {

        response = new MockHttpServletResponse();
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);
        KeyPair wrongKey = Keys.keyPairFor(SignatureAlgorithm.ES256);
        List<String> protectedHeaders = new ArrayList<String>();
        protectedHeaders.add("X-BATCH-RELEASE-TIME");
        SignatureResponseWrapper signatureResponseWrapper = new SignatureResponseWrapper(response, keyPair, 21, protectedHeaders);
        signatureResponseWrapper.getOutputStream().print("TEST");
        signatureResponseWrapper.flushBuffer();
        String digest = response.getHeader("Digest");
        String rawJWT = response.getHeader("Signature");
        assertNotNull(digest);
        String expected = "sha-256=" + Hex.encodeHexString(MessageDigest.getInstance("SHA-256").digest("TEST".getBytes()));
        assertEquals(expected, digest);
        
    }

    @Test
    public void testSignatureViaOutput() throws IOException, NoSuchAlgorithmException {
        response = new MockHttpServletResponse();
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);
        KeyPair wrongKey = Keys.keyPairFor(SignatureAlgorithm.ES256);
        List<String> protectedHeaders = new ArrayList<String>();
        protectedHeaders.add("X-BATCH-RELEASE-TIME");
        SignatureResponseWrapper signatureResponseWrapper = new SignatureResponseWrapper(response, keyPair, 21, protectedHeaders);
        signatureResponseWrapper.getOutputStream().print("TEST");
        OutputStream stream = OutputStream.nullOutputStream();
        signatureResponseWrapper.outputData(stream);
        String digest = response.getHeader("Digest");
        String rawJWT = response.getHeader("Signature");
        assertNotNull(digest);
        String expected = "sha-256=" + Hex.encodeHexString(MessageDigest.getInstance("SHA-256").digest("TEST".getBytes()));
        assertEquals(expected, digest);
    }

    @Test
    public void testBatchReleaseTime() throws IOException, NoSuchAlgorithmException{
        response = new MockHttpServletResponse();
        response.setHeader("X-BATCH-RELEASE-TIME", Long.toString(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli()));
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);
        KeyPair wrongKey = Keys.keyPairFor(SignatureAlgorithm.ES256);
        List<String> protectedHeaders = new ArrayList<String>();
        protectedHeaders.add("X-BATCH-RELEASE-TIME");
        SignatureResponseWrapper signatureResponseWrapper = new SignatureResponseWrapper(response, keyPair, 21, protectedHeaders);
        signatureResponseWrapper.getOutputStream().print("TEST");
        signatureResponseWrapper.flushBuffer();
        String digest = response.getHeader("Digest");
        String rawJWT = response.getHeader("Signature");
        JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(keyPair.getPublic()).build();
        Jwt jwt = jwtParser.parse(rawJWT);
    
        assertTrue(response.containsHeader("X-BATCH-RELEASE-TIME"));
        Claims claims = (Claims)jwt.getBody();
        assertTrue(claims.containsKey("batch-release-time"));
    }

    @Test
    public void testBatchReleaseTimeWithOutputStream() throws IOException, NoSuchAlgorithmException{
        response = new MockHttpServletResponse();
        response.setHeader("X-BATCH-RELEASE-TIME", Long.toString(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli()));
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);
        KeyPair wrongKey = Keys.keyPairFor(SignatureAlgorithm.ES256);
        List<String> protectedHeaders = new ArrayList<String>();
        protectedHeaders.add("X-BATCH-RELEASE-TIME");
        SignatureResponseWrapper signatureResponseWrapper = new SignatureResponseWrapper(response, keyPair, 21, protectedHeaders);
        signatureResponseWrapper.getOutputStream().print("TEST");
        OutputStream stream = OutputStream.nullOutputStream();
        signatureResponseWrapper.outputData(stream);
        String digest = response.getHeader("Digest");
        String rawJWT = response.getHeader("Signature");
        JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(keyPair.getPublic()).build();
        Jwt jwt = jwtParser.parse(rawJWT);
    
        assertTrue(response.containsHeader("X-BATCH-RELEASE-TIME"));
        Claims claims = (Claims)jwt.getBody();
        assertTrue(claims.containsKey("batch-release-time"));
    }

}