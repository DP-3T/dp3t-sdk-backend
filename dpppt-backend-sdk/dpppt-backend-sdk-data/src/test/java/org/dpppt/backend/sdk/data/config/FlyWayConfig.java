package org.dpppt.backend.sdk.data.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
public class FlyWayConfig {

	@Autowired
    DataSource dataSource;

	@Bean
	@Profile("hsqldb")
	public Flyway flyway() {
		Flyway flyWay = Flyway.configure().dataSource(dataSource).locations("classpath:/db/migration/hsqldb").load();
		flyWay.migrate();
		return flyWay;
	}

	@Bean
	@Profile("postgres")
	public Flyway flywayPostgres() {
		Flyway flyWay = Flyway.configure().dataSource(dataSource).locations("classpath:/db/migration/pgsql").load();
		flyWay.migrate();
		return flyWay;
	}
}
