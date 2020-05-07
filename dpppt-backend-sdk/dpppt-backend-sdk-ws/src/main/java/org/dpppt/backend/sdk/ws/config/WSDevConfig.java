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

import javax.sql.DataSource;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemReader;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import io.jsonwebtoken.SignatureAlgorithm;

@Configuration
@Profile("dev")
public class WSDevConfig extends WSBaseConfig {

		
	@Value("${ws.ecdsa.credentials.privateKey:}")
	private String privateKey;
	
	@Value("${ws.ecdsa.credentials.publicKey:}")
    public String publicKey;

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
		Security.setProperty("crypto.policy", "unlimited");
		if(privateKey.isEmpty() || publicKey.isEmpty()) {
			return super.getKeyPair(algorithm);
		}
		return new KeyPair(loadPublicKeyFromString(),loadPrivateKeyFromString());
	}

	private PrivateKey loadPrivateKeyFromString() {
		try {
			String privateKey = getPrivateKey();
			var reader = new StringReader(privateKey);
			var readerPem = new PemReader(reader);
			var obj = readerPem.readPemObject();
			var pkcs8KeySpec = new PKCS8EncodedKeySpec(obj.getContent());
			var kf = KeyFactory.getInstance("EC");
			return (PrivateKey) kf.generatePrivate(pkcs8KeySpec);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}

	private PublicKey loadPublicKeyFromString() {
		try {
			var reader = new StringReader(getPublicKey());
			var readerPem = new PemReader(reader);
			var obj = readerPem.readPemObject();
			return KeyFactory.getInstance("EC").generatePublic(
					new X509EncodedKeySpec(obj.getContent())
			);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}

    String getPrivateKey() {
        return new String(Base64.getDecoder().decode(privateKey));
    }

    String getPublicKey() {
        return new String(Base64.getDecoder().decode(publicKey));
    }


}
