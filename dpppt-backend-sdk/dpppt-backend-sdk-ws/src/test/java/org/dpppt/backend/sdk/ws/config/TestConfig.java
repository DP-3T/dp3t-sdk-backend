package org.dpppt.backend.sdk.ws.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration("test-config")
public class TestConfig {
    @Autowired
    DataSource dataSource;
    @Bean
	public PlatformTransactionManager testTransactionManager() throws Exception {
		DataSourceTransactionManager dstm = new DataSourceTransactionManager(dataSource);
		return dstm;
	}
}