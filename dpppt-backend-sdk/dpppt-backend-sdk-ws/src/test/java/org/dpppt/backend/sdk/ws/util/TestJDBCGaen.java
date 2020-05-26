/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.backend.sdk.ws.util;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class TestJDBCGaen {
    private static final String PGSQL = "pgsql";
	private final String dbType;
	private final NamedParameterJdbcTemplate jt;

	public TestJDBCGaen(String dbType, DataSource dataSource) {
		this.dbType = dbType;
		this.jt = new NamedParameterJdbcTemplate(dataSource);
    }
    
    @Transactional(readOnly = false)
	public void upsertExposees(List<GaenKey> gaenKeys, OffsetDateTime receivedAt) {
		String sql = null;
		if (dbType.equals(PGSQL)) {
			sql = "insert into t_gaen_exposed (key, rolling_start_number, rolling_period, transmission_risk_level, received_at) values (:key, :rolling_start_number, :rolling_period, :transmission_risk_level, :received_at)"
					+ " on conflict on constraint gaen_exposed_key do nothing";
		} else {
			sql = "merge into t_gaen_exposed using (values(cast(:key as varchar(24)), :rolling_start_number, :rolling_period, :transmission_risk_level, :received_at))"
					+ " as vals(key, rolling_start_number, rolling_period, transmission_risk_level, received_at) on t_gaen_exposed.key = vals.key"
					+ " when not matched then insert (key, rolling_start_number, rolling_period, transmission_risk_level, received_at) values (vals.key, vals.rolling_start_number, vals.rolling_period, vals.transmission_risk_level, vals.received_at)";
		}
		var parameterList = new ArrayList<MapSqlParameterSource>();
		for(var gaenKey : gaenKeys) {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("key", gaenKey.getKeyData());
			params.addValue("rolling_start_number", gaenKey.getRollingStartNumber());
			params.addValue("rolling_period", gaenKey.getRollingPeriod());
            params.addValue("transmission_risk_level", gaenKey.getTransmissionRiskLevel());
            params.addValue("received_at", Date.from((receivedAt.toInstant())));
			parameterList.add(params);
		}
		jt.batchUpdate(sql, parameterList.toArray(new MapSqlParameterSource[0]));
	}
}