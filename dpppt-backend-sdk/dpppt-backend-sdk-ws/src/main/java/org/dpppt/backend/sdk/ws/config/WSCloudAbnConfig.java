/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.ws.config;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud-abn")
public class WSCloudAbnConfig extends WSCloudBaseConfig {
  @Value("${vcap.services.ecdsa_abn.credentials.privateKey}")
  private String privateKey;

  @Value("${vcap.services.ecdsa_abn.credentials.publicKey}")
  public String publicKey;

  @Override
  String getPrivateKey() {
    return new String(Base64.getDecoder().decode(privateKey));
  }

  @Override
  String getPublicKey() {
    return new String(Base64.getDecoder().decode(publicKey));
  }

  @Override
  public String getBundleId() {
    return "ch.admin.bag.dp3t.abn";
  }

  @Override
  public String getPackageName() {
    return "ch.admin.bag.dp3t";
  }
}
