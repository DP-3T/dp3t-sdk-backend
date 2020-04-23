/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.util;

import java.io.IOException;
import java.net.URL;

import org.dpppt.backend.sdk.model.keycloak.KeyCloakPublicKey;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author bachmann created on 22.04.20
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
