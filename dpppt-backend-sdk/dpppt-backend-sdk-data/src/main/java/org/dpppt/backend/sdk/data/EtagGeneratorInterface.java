/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.data;

public interface EtagGeneratorInterface {
    public String getEtag(int primaryKey);
}