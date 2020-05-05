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

import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Configuration
public abstract class WSCloudBaseConfig extends WSBaseConfig {

	@Autowired
	private DataSource dataSource;

	abstract String getPublicKey();
	abstract String getPrivateKey();

	@Value("${ws.cloud.base.config.publicKey.fromCertificate:true}")
	private boolean publicKeyFromCertificate;

	@Override
	public DataSource dataSource() {
		return dataSource;
	}

	@Bean
	@Override
	public Flyway flyway() {
		Flyway flyWay = Flyway.configure().dataSource(dataSource()).locations("classpath:/db/migration/pgsql_cluster").load();
		flyWay.migrate();
		return flyWay;

	}

	@Override
	public String getDbType() {
		return "pgsql";
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

	}
	@Override
	public KeyPair getKeyPair(SignatureAlgorithm algorithm) {
		Security.addProvider(new BouncyCastleProvider());
		Security.setProperty("crypto.policy", "unlimited");
		return new KeyPair(loadPublicKeyFromString(),loadPrivateKeyFromString());
	}

	private PrivateKey loadPrivateKeyFromString() {
		try {
			String privateKey = getPrivateKey();
			Reader reader = new StringReader(privateKey);
			PemReader readerPem = new PemReader(reader);
			PemObject obj = readerPem.readPemObject();
			PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(obj.getContent());
			KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
			return (PrivateKey) kf.generatePrivate(pkcs8KeySpec);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}

	private PublicKey loadPublicKeyFromString() {
		try {
			if(publicKeyFromCertificate) {
				return CertificateFactory
						.getInstance("X.509")
						.generateCertificate(new ByteArrayInputStream(getPublicKey().getBytes()))
						.getPublicKey();
			} else {
				Reader reader = new StringReader(getPublicKey());
				PemReader readerPem = new PemReader(reader);
				PemObject obj = readerPem.readPemObject();
				return KeyFactory.getInstance("EC").generatePublic(
						new X509EncodedKeySpec(obj.getContent())
				);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException();
		}
	}
}
