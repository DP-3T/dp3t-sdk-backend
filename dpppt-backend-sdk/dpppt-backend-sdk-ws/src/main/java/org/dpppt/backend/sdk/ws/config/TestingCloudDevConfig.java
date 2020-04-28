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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import io.jsonwebtoken.SignatureAlgorithm;

@Configuration
@Profile("test-cloud")
public class TestingCloudDevConfig  extends WSBaseConfig{


	private String privateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgx18xx3XadMzGCunnoUjpiCt1rZ81I4XAJbRaQi0eZbKgCgYIKoZIzj0DAQehRANCAARzqfUU3PpGfA140z0f4rFo9ySsHGApTv32/4NPLyK9zVkzecPJernNTYAVe+C8sXNbFT3P0UJwM8ZNw3ty87Jf";

	public String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEc6n1FNz6RnwNeNM9H+KxaPckrBxgKU799v+DTy8ivc1ZM3nDyXq5zU2AFXvgvLFzWxU9z9FCcDPGTcN7cvOyXw==";
	@Bean
	@Override
	public DataSource dataSource() {
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

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

	}
	@Override
	public KeyPair getKeyPair(SignatureAlgorithm algorithm) {
		return new KeyPair(loadPublicKeyFromString(),loadPrivateKeyFromString());
	}

	private PrivateKey loadPrivateKeyFromString() {
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
		try {
			KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
			return (PrivateKey) kf.generatePrivate(pkcs8KeySpec);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private PublicKey loadPublicKeyFromString() {
		X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
		try {
			KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
			return (PublicKey) kf.generatePublic(keySpecX509);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}