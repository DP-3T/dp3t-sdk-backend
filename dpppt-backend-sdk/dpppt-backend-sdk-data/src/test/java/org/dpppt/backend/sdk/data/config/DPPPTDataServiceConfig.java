package org.dpppt.backend.sdk.data.config;


import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.JDBCDPPPTDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DPPPTDataServiceConfig {

    @Autowired
    DataSource dataSource;

    @Autowired
    String dbType;

    @Bean
    public DPPPTDataService DPPPTDataService() {
        return new JDBCDPPPTDataServiceImpl(dbType, dataSource);
    }

}
