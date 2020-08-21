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

import org.dpppt.backend.sdk.ws.security.KeyVault;
import org.dpppt.backend.sdk.ws.security.KeyVault.PrivateKeyNoSuitableEncodingFoundException;
import org.dpppt.backend.sdk.ws.security.KeyVault.PublicKeyNoSuitableEncodingFoundException;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class WSCloudBaseConfig extends WSBaseConfig {

	abstract String getPublicKey();

	abstract String getPrivateKey();

	@Value("${ws.cloud.base.config.publicKey.fromCertificate:true}")
	private boolean publicKeyFromCertificate;

	@Value("${datasource.maximumPoolSize}")
	int dataSourceMaximumPoolSize;

	@Value("${datasource.connectionTimeout}")
	int dataSourceConnectionTimeout;

	@Value("${datasource.leakDetectionThreshold:0}")
	int dataSourceLeakDetectionThreshold;

	@Bean
	@Override
	public DataSource dataSource() {
		PoolConfig poolConfig = new PoolConfig(dataSourceMaximumPoolSize, dataSourceConnectionTimeout);
		DataSourceConfig dbConfig = new DataSourceConfig(poolConfig, null);
		dbConfig.getConnectionProperties().getConnectionProperties().put("leakDetectionThreshold",
				dataSourceLeakDetectionThreshold);
		CloudFactory factory = new CloudFactory();
		return factory.getCloud().getSingletonServiceConnector(DataSource.class, dbConfig);
	}

	@Bean
	@Override
	public Flyway flyway() {
		Flyway flyWay = Flyway.configure().dataSource(dataSource()).locations("classpath:/db/migration/pgsql_cluster")
				.load();
		flyWay.migrate();
		return flyWay;

	}

	@Override
	public String getDbType() {
		return "pgsql";
	}

	@Bean
	protected KeyVault keyVault() {
		var privateKey = getPrivateKey();
		var publicKey = getPublicKey();

		if (privateKey.isEmpty() || publicKey.isEmpty()) {
			var kp = super.getKeyPair(algorithm);
			var gaenKp = new KeyVault.KeyVaultKeyPair("gaen", kp);
			var nextDayJWTKp = new KeyVault.KeyVaultKeyPair("nextDayJWT", kp);
			var hashFilterKp = new KeyVault.KeyVaultKeyPair("hashFilter", kp);
			return new KeyVault(gaenKp, nextDayJWTKp, hashFilterKp);
		}

		var gaen = new KeyVault.KeyVaultEntry("gaen", getPrivateKey(), getPublicKey(), "EC");
		var nextDayJWT = new KeyVault.KeyVaultEntry("nextDayJWT", getPrivateKey(), getPublicKey(), "EC");
		var hashFilter = new KeyVault.KeyVaultEntry("hashFilter", getPrivateKey(), getPublicKey(), "EC");

		try {
			return new KeyVault(gaen, nextDayJWT, hashFilter);
		} catch (PrivateKeyNoSuitableEncodingFoundException | PublicKeyNoSuitableEncodingFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
