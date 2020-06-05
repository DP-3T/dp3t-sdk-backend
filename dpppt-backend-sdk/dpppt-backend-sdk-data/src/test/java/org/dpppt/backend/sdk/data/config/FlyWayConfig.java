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
