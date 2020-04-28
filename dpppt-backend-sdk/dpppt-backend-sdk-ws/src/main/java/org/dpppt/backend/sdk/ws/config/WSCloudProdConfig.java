package org.dpppt.backend.sdk.ws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud-prod")
public class WSCloudProdConfig extends WSCloudBaseConfig {
	@Value("${vcap.services.ecdsa_prod.credentials.privateKey}")
	private String privateKey;
	@Value("${vcap.services.ecdsa_prod.credentials.publicKey}")
    public String publicKey;
    
    @Override
    String getPrivateKey() {
        return privateKey;
    }
    @Override
    String getPublicKey() {
        return publicKey;
    }
}