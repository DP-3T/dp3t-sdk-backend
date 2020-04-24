/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.data;

import java.nio.ByteBuffer;

import org.springframework.util.DigestUtils;

public class EtagGenerator implements EtagGeneratorInterface {
    private byte secret[] = new byte[]{'s', 'e', 'c', 'r', 'e', 't'};
    @Override
    public String getEtag(int primaryKey, String contentType) {
        String hash = DigestUtils.md5DigestAsHex(ByteBuffer.allocate(10).putInt(primaryKey).put(secret).array());
        return contentType +  hash;
    }

}