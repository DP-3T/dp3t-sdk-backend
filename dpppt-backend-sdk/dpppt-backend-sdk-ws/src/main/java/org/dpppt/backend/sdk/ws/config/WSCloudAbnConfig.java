package org.dpppt.backend.sdk.ws.config;

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
        return privateKey;
    }
    @Override
    String getPublicKey() {
        return publicKey;
    }
}