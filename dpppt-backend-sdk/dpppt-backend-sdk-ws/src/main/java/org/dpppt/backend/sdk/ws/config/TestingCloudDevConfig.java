/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */
package org.dpppt.backend.sdk.ws.config;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import io.jsonwebtoken.SignatureAlgorithm;

@Configuration
@Profile("test-cloud")
public class TestingCloudDevConfig  extends WSCloudBaseConfig{

	@Value("${vcap.services.ecdsa_dev.credentials.privateKey}")
	private String privateKey;
	@Value("${vcap.services.ecdsa_dev.credentials.publicKey}")
    public String publicKey;
    
	@Override
    String getPrivateKey() {
        return new String(Base64.getDecoder().decode(privateKey));
    }
    @Override
    String getPublicKey() {
        return new String(Base64.getDecoder().decode(publicKey));
    }

	@Bean
	DataSource hsqlSource() {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build();
	}
	

	@Bean
	@Override
	public Flyway flyway() {
		Flyway flyWay = Flyway.configure().dataSource(dataSource()).locations("classpath:/db/migration/hsqldb").load();
		flyWay.migrate();
		return flyWay;
	}

	@Override
	public String getDbType() {
		return "hsqldb";
	}
}