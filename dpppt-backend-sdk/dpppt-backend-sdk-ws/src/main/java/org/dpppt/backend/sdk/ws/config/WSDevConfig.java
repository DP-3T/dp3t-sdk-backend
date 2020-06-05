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

import java.util.Base64;

import javax.sql.DataSource;

import org.dpppt.backend.sdk.ws.security.KeyVault;
import org.dpppt.backend.sdk.ws.security.KeyVault.PrivateKeyNoSuitableEncodingFoundException;
import org.dpppt.backend.sdk.ws.security.KeyVault.PublicKeyNoSuitableEncodingFoundException;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

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
		return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL).build();
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

	@Bean
	KeyVault keyVault() {
		var privateKey = getPrivateKey();
		var publicKey = getPublicKey();
		
		if(privateKey.isEmpty() || publicKey.isEmpty()) {
			var kp = super.getKeyPair(algorithm);
			var gaenKp = new KeyVault.KeyVaultKeyPair("gaen", kp);
			var nextDayJWTKp = new KeyVault.KeyVaultKeyPair("nextDayJWT", kp);
			var hashFilterKp = new KeyVault.KeyVaultKeyPair("hashFilter", kp);
			return new KeyVault(gaenKp, nextDayJWTKp, hashFilterKp);
		}
		else {
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

    String getPrivateKey() {
        return new String(Base64.getDecoder().decode(privateKey));
    }

    String getPublicKey() {
        return new String(Base64.getDecoder().decode(publicKey));
    }


}
