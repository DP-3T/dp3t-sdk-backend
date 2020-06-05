/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.backend.sdk.ws.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class KeyVaultTest {
    @Test
    public void testProviderNeedsToBeStatic() throws Exception {
    	byte[] test = "EK Export v1    ".getBytes();
        assertFalse(KeyVault.registerNewPublicEncodingProvider(KeyVaultTest.class, "testFunctionPublic1"));
        assertFalse(KeyVault.registerNewPrivateEncodingProvider(KeyVaultTest.class, "testFunctionPrivate1"));
    }

    @Test
    public void returnValueNeedsToBeCorrect() throws Exception {
        assertFalse(KeyVault.registerNewPublicEncodingProvider(KeyVaultTest.class, "testFunctionPublic2"));
        assertFalse(KeyVault.registerNewPrivateEncodingProvider(KeyVaultTest.class, "testFunctionPrivate2"));
    }

    @Test
    public void functionNeedsToExist() throws Exception {
        assertFalse(KeyVault.registerNewPublicEncodingProvider(KeyVaultTest.class, "testFunctionPublic0"));
        assertFalse(KeyVault.registerNewPrivateEncodingProvider(KeyVaultTest.class, "testFunctionPrivate0"));
    }

    @Test
    public void testJavaEncodingFile() throws Exception {
        var publicKey = IOUtils.toString(new ClassPathResource("/generated_pub_2.pem").getInputStream());
        var privateKey = IOUtils.toString(new ClassPathResource("/generated_private_2.pem").getInputStream());

        var entry = new KeyVault.KeyVaultEntry("test", new String(privateKey), new String(publicKey), "RSA");
        var keyVault = new KeyVault(entry);
        assertNotNull(keyVault.get("test"));
    }

    @Test
    public void testPEMFile() throws Exception {
        var publicKey = Base64.getDecoder().decode(IOUtils.toString(new ClassPathResource("/generated_pub_3.pem").getInputStream()));
        var privateKey = Base64.getDecoder().decode(IOUtils.toString(new ClassPathResource("/generated_private_3.pem").getInputStream()));

        var entry = new KeyVault.KeyVaultEntry("test", new String(privateKey), new String(publicKey), "EC");
        var keyVault = new KeyVault(entry);
        assertNotNull(keyVault.get("test"));
    }

    public PublicKey testFunctionPublic1(String part, String algo) {
        return null;
    }
    public PrivateKey testFunctionPrivate1(String part, String algo) {
        return null;
    }

    public String testFunctionPublic2(String part, String algo) {
        return null;
    }
    public String testFunctionPrivate2(String part, String algo) {
        return null;
    }
}