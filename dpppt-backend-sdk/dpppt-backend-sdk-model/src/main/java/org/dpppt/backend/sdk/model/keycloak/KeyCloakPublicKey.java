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
