package org.dpppt.backend.sdk.ws.security.signature;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        SignatureResponseWrapper signatureResponseWrapper = new SignatureResponseWrapper(response, keyPair, 21, Collections.emptyList());
        signatureResponseWrapper.getOutputStream().print("TEST");
        signatureResponseWrapper.flushBuffer();
        String digest = response.getHeader("Digest");
        String rawJWT = response.getHeader("Signature");
        assertNotNull(digest);
        String expected = "sha-256=" + Hex.encodeHexString(MessageDigest.getInstance("SHA-256").digest("TEST".getBytes()));
        assertEquals(expected, digest);
        JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(keyPair.getPublic()).build();
        Jwt jwt = jwtParser.parse(rawJWT);
        assertThrows(SignatureException.class, () -> {
            Jwts.parserBuilder().setSigningKey(wrongKey.getPublic()).build().parse(rawJWT);
        });
    }

}