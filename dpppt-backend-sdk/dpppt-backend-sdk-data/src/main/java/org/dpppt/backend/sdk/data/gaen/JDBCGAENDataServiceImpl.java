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

	public JDBCGAENDataServiceImpl(String dbType, DataSource dataSource) {
		this.dbType = dbType;
		this.jt = new NamedParameterJdbcTemplate(dataSource);
	}

	@Override
	@Transactional(readOnly = false)
	public void upsertExposees(List<GaenKey> gaenKeys) {
		// TODO Auto-generated method stub
	}

	@Override
	@Transactional(readOnly = true)
	public int getMaxExposedIdForBatchReleaseTime(Long batchReleaseTime, long batchLength) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("batchReleaseTime", Date.from(Instant.ofEpochMilli(batchReleaseTime)));
		params.addValue("startBatch", Date.from(Instant.ofEpochMilli(batchReleaseTime - batchLength)));
		String sql = "select max(pk_exposed_id) from t_gaen_exposed where received_at >= :startBatch and received_at < :batchReleaseTime";
		Integer maxId = jt.queryForObject(sql, params, Integer.class);
		if (maxId == null) {
			return 0;
		} else {
			return maxId;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<GaenKey> getSortedExposedForBatchReleaseTime(Long batchReleaseTime, long batchLength) {
		// TODO Auto-generated method stub
		return new ArrayList<>();
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
