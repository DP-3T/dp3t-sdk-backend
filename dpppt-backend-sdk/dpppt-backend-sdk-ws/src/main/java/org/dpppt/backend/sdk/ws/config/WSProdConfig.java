/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@Profile("prod")
public class WSProdConfig extends WSBaseConfig {
	
	@Value("${datasource.username}")
	String dataSourceUser;

	@Value("${datasource.password}")
	String dataSourcePassword;

	@Value("${datasource.url}")
	String dataSourceUrl;

	@Value("${datasource.driverClassName}")
	String dataSourceDriver;

	@Value("${datasource.failFast}")
	String dataSourceFailFast;

	@Value("${datasource.maximumPoolSize}")
	String dataSourceMaximumPoolSize;

	@Value("${datasource.maxLifetime}")
	String dataSourceMaxLifetime;

	@Value("${datasource.idleTimeout}")
	String dataSourceIdleTimeout;

	@Value("${datasource.connectionTimeout}")
	String dataSourceConnectionTimeout;

	@Bean(destroyMethod = "close")
	public DataSource dataSource() {
		HikariConfig config = new HikariConfig();
		Properties props = new Properties();
		props.put("url", dataSourceUrl);
		props.put("user", dataSourceUser);
		props.put("password", dataSourcePassword);
		config.setDataSourceProperties(props);
		config.setDataSourceClassName(dataSourceDriver);
		config.setMaximumPoolSize(Integer.parseInt(dataSourceMaximumPoolSize));
		config.setMaxLifetime(Integer.parseInt(dataSourceMaxLifetime));
		config.setIdleTimeout(Integer.parseInt(dataSourceIdleTimeout));
		config.setConnectionTimeout(Integer.parseInt(dataSourceConnectionTimeout));
		return new HikariDataSource(config);
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

}
