package org.dpppt.backend.sdk.data.config;

import org.dpppt.backend.sdk.data.util.SingletonPostgresContainer;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("postgres")
public class PostgresDataConfig {

    @Bean
    public DataSource dataSource() {
        final SingletonPostgresContainer instance = SingletonPostgresContainer.getInstance();
        instance.start();

        return DataSourceBuilder.create()
            .driverClassName(instance.getDriverClassName())
            .url(instance.getJdbcUrl())
            .username(instance.getUsername())
            .password(instance.getPassword())
            .build();
    }

    @Bean
    public String dbType() {
        return "pgsql";
    }

}
