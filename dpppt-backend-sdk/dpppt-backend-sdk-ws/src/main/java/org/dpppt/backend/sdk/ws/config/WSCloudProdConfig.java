package org.dpppt.backend.sdk.ws.config;

import java.util.Base64;

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
        return new String(Base64.getDecoder().decode(privateKey));
    }
    @Override
    String getPublicKey() {
        return new String(Base64.getDecoder().decode(publicKey));
    }
}