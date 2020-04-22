package org.dpppt.backend.sdk.ws.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dpppt.backend.sdk.model.keycloak.KeyCloakPublicKey;

import java.io.IOException;
import java.net.URL;

/**
 * @author bachmann
 * created on 22.04.20
 **/
public class KeyHelper {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private KeyHelper() {
    }

    public static String getPublicKeyFromKeycloak(String url) throws IOException {
        URL jsonUrl = new URL(url);
        KeyCloakPublicKey publicKey = objectMapper.readValue(jsonUrl, KeyCloakPublicKey.class);
        return publicKey.getPublicKey();
    }
}
