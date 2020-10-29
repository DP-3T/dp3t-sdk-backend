/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.model.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KeyCloakPublicKey {
  private String realm;

  @JsonProperty("public_key")
  private String publicKey;

  @JsonProperty("token-service")
  private String tokenService;

  @JsonProperty("account-service")
  private String accountService;

  @JsonProperty("tokens-not-before")
  private String tokensNotBefore;

  public String getRealm() {
    return realm;
  }

  public void setRealm(String realm) {
    this.realm = realm;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public String getTokenService() {
    return tokenService;
  }

  public void setTokenService(String tokenService) {
    this.tokenService = tokenService;
  }

  public String getAccountService() {
    return accountService;
  }

  public void setAccountService(String accountService) {
    this.accountService = accountService;
  }

  public String getTokensNotBefore() {
    return tokensNotBefore;
  }

  public void setTokensNotBefore(String tokensNotBefore) {
    this.tokensNotBefore = tokensNotBefore;
  }
}
