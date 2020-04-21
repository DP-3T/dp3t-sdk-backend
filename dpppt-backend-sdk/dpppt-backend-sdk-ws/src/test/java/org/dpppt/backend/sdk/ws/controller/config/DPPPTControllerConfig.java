package org.dpppt.backend.sdk.ws.controller.config;

import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.EtagGenerator;
import org.dpppt.backend.sdk.data.EtagGeneratorInterface;
import org.dpppt.backend.sdk.data.JDBCDPPPTDataServiceImpl;
import org.dpppt.backend.sdk.ws.controller.DPPPTController;
import org.dpppt.backend.sdk.ws.security.JWTValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@Configuration
public class DPPPTControllerConfig {
    @Bean
    public DPPPTController dppptSDKController() {
        return new DPPPTController(dppptSDKDataService(), etagGenerator(), "org.dpppt.demo", 5,requestValidator());
    }

    @Bean 
    public ValidateRequest requestValidator() {
        return new JWTValidateRequest();
    }

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build();
    }

    @Bean
    public Flyway flyway() {
        Flyway flyWay = Flyway.configure().dataSource(dataSource()).locations("classpath:/db/migration/hsqldb").load();
        flyWay.migrate();
        return flyWay;
    }

    public String getDbType() {
        return "hsqldb";
    }

    @Bean
    public DPPPTDataService dppptSDKDataService() {
        return new JDBCDPPPTDataServiceImpl(getDbType(), dataSource());
    }

    @Bean
    public EtagGeneratorInterface etagGenerator() {
        return new EtagGenerator();
    }
}
