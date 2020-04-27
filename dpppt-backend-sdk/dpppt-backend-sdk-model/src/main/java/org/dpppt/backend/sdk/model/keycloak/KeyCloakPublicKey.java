package org.dpppt.backend.sdk.model.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
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
}
