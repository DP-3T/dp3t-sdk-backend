/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.data.config;


import org.dpppt.backend.sdk.data.DPPPTDataService;
import org.dpppt.backend.sdk.data.JDBCDPPPTDataServiceImpl;
import org.dpppt.backend.sdk.data.JDBCRedeemDataServiceImpl;
import org.dpppt.backend.sdk.data.RedeemDataService;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.data.gaen.JDBCGAENDataServiceImpl;
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

    @Bean
    public GAENDataService gaenDataService() {
        return new JDBCGAENDataServiceImpl(dbType, dataSource);
    }
    @Bean
    public RedeemDataService redeemDataService() {
        return new JDBCRedeemDataServiceImpl(dataSource);
    }
}
