/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.data;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class EtagGenerator implements EtagGeneratorInterface {

    private byte secret[] = new byte[]{'s', 'e', 'c', 'r', 'e', 't'};

    @Override
    public String getEtag(int primaryKey, String contentType) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            ByteBuffer byteBuffer = ByteBuffer.allocate(secret.length + Integer.BYTES);
            byteBuffer.put(secret);
            byteBuffer.putInt(primaryKey);

            final byte[] saltedPayload = byteBuffer.array();

            final byte[] sha256Hash = messageDigest.digest(saltedPayload);

            return contentType + Base64.getEncoder().encodeToString(sha256Hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
