package org.dpppt.backend.sdk.ws.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import io.jsonwebtoken.SignatureAlgorithm;

@Configuration
@Profile("cloud")
public class WSCloudConfig extends WSBaseConfig {

	@Autowired
	private DataSource dataSource;

	@Override
	public DataSource dataSource() {
		return dataSource;
	}

	@Bean
	@Override
	public Flyway flyway() {
		Flyway flyWay = Flyway.configure().dataSource(dataSource()).locations("classpath:/db/migration/pgsql").load();
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
		try {
			return KeyPairGenerator.getInstance("RSA").generateKeyPair();
		}
		catch(Exception ex) {
			return null;
		}
	}
}
