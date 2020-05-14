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

import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.Test;

public class KeyVaultTest {
    @Test
    public void testProviderNeedsToBeStatic() throws Exception {
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