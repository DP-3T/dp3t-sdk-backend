/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.data.gaen;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class JDBCGAENDataServiceImpl implements GAENDataService {

	private static final Logger logger = LoggerFactory.getLogger(JDBCGAENDataServiceImpl.class);

	private static final String PGSQL = "pgsql";
	private final String dbType;
	private final NamedParameterJdbcTemplate jt;
	private final Duration bucketLength;

	public JDBCGAENDataServiceImpl(String dbType, DataSource dataSource, Duration bucketLength) {
		this.dbType = dbType;
		this.jt = new NamedParameterJdbcTemplate(dataSource);
		this.bucketLength = bucketLength;
	}

	@Override
	@Transactional(readOnly = false)
	public void upsertExposees(List<GaenKey> gaenKeys) {
		String sql = null;
		if (dbType.equals(PGSQL)) {
			sql = "insert into t_gaen_exposed (key, rolling_start_number, rolling_period, transmission_risk_level, received_at) values (:key, :rolling_start_number, :rolling_period, :transmission_risk_level, :received_at)"
					+ " on conflict on constraint gaen_exposed_key do nothing";
		} else {
			sql = "merge into t_gaen_exposed using (values(cast(:key as varchar(24)), :rolling_start_number, :rolling_period, :transmission_risk_level, :received_at))"
					+ " as vals(key, rolling_start_number, rolling_period, transmission_risk_level, received_at) on t_gaen_exposed.key = vals.key"
					+ " when not matched then insert (key, rolling_start_number, rolling_period, transmission_risk_level, received_at) values (vals.key, vals.rolling_start_number, vals.rolling_period, transmission_risk_level, vals.received_at)";
		}
		var parameterList = new ArrayList<MapSqlParameterSource>();
		var nowMillis = System.currentTimeMillis();
		var receivedAt = (nowMillis/bucketLength.toMillis() + 1) * bucketLength.toMillis() - 1;
		for (var gaenKey : gaenKeys) {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("key", gaenKey.getKeyData());
			params.addValue("rolling_start_number", gaenKey.getRollingStartNumber());
			params.addValue("rolling_period", gaenKey.getRollingPeriod());
			params.addValue("transmission_risk_level", gaenKey.getTransmissionRiskLevel());
			params.addValue("received_at", Date.from(Instant.ofEpochMilli(receivedAt)));
			
			parameterList.add(params);
		}
		jt.batchUpdate(sql, parameterList.toArray(new MapSqlParameterSource[0]));
	}

	@Override
	@Transactional(readOnly = true)
	public int getMaxExposedIdForKeyDate(Long keyDate, Long publishedAfter, Long publishedUntil) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("rollingPeriodStartNumberStart",
				GaenUnit.TenMinutes.between(Instant.ofEpochMilli(0), Instant.ofEpochMilli(keyDate)));
		params.addValue("rollingPeriodStartNumberEnd", GaenUnit.TenMinutes.between(Instant.ofEpochMilli(0),
				Instant.ofEpochMilli(keyDate).atOffset(ZoneOffset.UTC).plusDays(1).toInstant()));
		params.addValue("publishedUntil", new Date(publishedUntil));
		
		String sql = "select max(pk_exposed_id) from t_gaen_exposed where"
				+ " rolling_start_number >= :rollingPeriodStartNumberStart"
				+ " and rolling_start_number < :rollingPeriodStartNumberEnd"
				+ " and received_at < :publishedUntil";
		
		if (publishedAfter != null) {
			params.addValue("publishedAfter", new Date(publishedAfter));
			sql += " and received_at >= :publishedAfter";
		}

		Integer maxId = jt.queryForObject(sql, params, Integer.class);
		if (maxId == null) {
			return 0;
		} else {
			return maxId;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<GaenKey> getSortedExposedForKeyDate(Long keyDate, Long publishedAfter, Long publishedUntil) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("rollingPeriodStartNumberStart",
				GaenUnit.TenMinutes.between(Instant.ofEpochMilli(0), Instant.ofEpochMilli(keyDate)));
		params.addValue("rollingPeriodStartNumberEnd", GaenUnit.TenMinutes.between(Instant.ofEpochMilli(0),
				Instant.ofEpochMilli(keyDate).atOffset(ZoneOffset.UTC).plusDays(1).toInstant()));
		params.addValue("publishedUntil", new Date(publishedUntil));

		String sql = "select pk_exposed_id, key, rolling_start_number, rolling_period, transmission_risk_level from t_gaen_exposed where"
				+ " rolling_start_number >= :rollingPeriodStartNumberStart"
				+ " and rolling_start_number < :rollingPeriodStartNumberEnd" 
				+ " and received_at < :publishedUntil";

		if (publishedAfter != null) {
			params.addValue("publishedAfter", new Date(publishedAfter));
			sql += " and received_at >= :publishedAfter";
		}
		
		sql += " order by pk_exposed_id desc";
		
		return jt.query(sql, params, new GaenKeyRowMapper());
	}

	@Override
	@Transactional(readOnly = false)
	public void cleanDB(Duration retentionPeriod) {
		OffsetDateTime retentionTime = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
				.minus(retentionPeriod);
		logger.info("Cleanup DB entries before: " + retentionTime);
		MapSqlParameterSource params = new MapSqlParameterSource("retention_time",
				Date.from(retentionTime.toInstant()));
		String sqlExposed = "delete from t_gaen_exposed where received_at < :retention_time";
		jt.update(sqlExposed, params);
	}

}
