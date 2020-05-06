/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.dpppt.backend.sdk.model.Exposee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JDBCDPPPTDataServiceImpl implements DPPPTDataService {

	private static final Logger logger = LoggerFactory.getLogger(JDBCDPPPTDataServiceImpl.class);
	private static final String PGSQL = "pgsql";
	private final String dbType;
	private final NamedParameterJdbcTemplate jt;
	private final SimpleJdbcInsert reedemUUIDInsert;

	public JDBCDPPPTDataServiceImpl(String dbType, DataSource dataSource) {
		this.dbType = dbType;
		this.jt = new NamedParameterJdbcTemplate(dataSource);
		this.reedemUUIDInsert = new SimpleJdbcInsert(dataSource).withTableName("t_redeem_uuid")
				.usingGeneratedKeyColumns("pk_redeem_uuid_id");
	}

	@Override
	@Transactional(readOnly = false)
	public void upsertExposee(Exposee exposee, String appSource) {
		String sql = null;
		if (dbType.equals(PGSQL)) {
			sql = "insert into t_exposed (key, key_date, app_source) values (:key, :key_date, :app_source)"
					+ " on conflict on constraint key do nothing";
		} else {
			sql = "merge into t_exposed using (values(cast(:key as varchar(10000)), cast(:key_date as date), cast(:app_source as varchar(50))))"
					+ " as vals(key, key_date, app_source) on t_exposed.key = vals.key"
					+ " when not matched then insert (key, key_date, app_source) values (vals.key, vals.key_date, vals.app_source)";
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("key", exposee.getKey());
		params.addValue("app_source", appSource);
		params.addValue("key_date", new Date(exposee.getKeyDate()));
		jt.update(sql, params);
	}
	@Override
	@Transactional(readOnly = false)
	public void upsertExposees(List<Exposee> exposees, String appSource) {
		String sql = null;
		if (dbType.equals(PGSQL)) {
			sql = "insert into t_exposed (key, key_date, app_source) values (:key, :key_date, :app_source)"
					+ " on conflict on constraint key do nothing";
		} else {
			sql = "merge into t_exposed using (values(cast(:key as varchar(10000)), cast(:key_date as date), cast(:app_source as varchar(50))))"
					+ " as vals(key, key_date, app_source) on t_exposed.key = vals.key"
					+ " when not matched then insert (key, key_date, app_source) values (vals.key, vals.key_date, vals.app_source)";
		}
		var parameterList = new ArrayList<MapSqlParameterSource>();
		for(var exposee : exposees) {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("key", exposee.getKey());
			params.addValue("app_source", appSource);
			params.addValue("key_date", new Date(exposee.getKeyDate()));
			parameterList.add(params);
		}
		jt.batchUpdate(sql, parameterList.toArray(new MapSqlParameterSource[0]));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Exposee> getSortedExposedForDay(OffsetDateTime day) {
		OffsetDateTime dayMidnight = day.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
		String sql = "select pk_exposed_id, key, key_date from t_exposed where received_at >= :dayMidnight and received_at < :nextDayMidnight order by pk_exposed_id desc";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("dayMidnight", dayMidnight);
		params.addValue("nextDayMidnight", dayMidnight.plusDays(1));
		return jt.query(sql, params, new ExposeeRowMapper());
	}

	@Override
	@Transactional(readOnly = true)
	public Integer getMaxExposedIdForDay(OffsetDateTime day) {
		OffsetDateTime dayMidnight = day.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("dayMidnight", dayMidnight);
		params.addValue("nextDayMidnight", dayMidnight.plusDays(1));
		String sql = "select max(pk_exposed_id) from t_exposed where received_at >= :dayMidnight and received_at < :nextDayMidnight";
		Integer maxId = jt.queryForObject(sql, params, Integer.class);
		if (maxId == null) {
			return 0;
		} else {
			return maxId;
		}
	}

	@Override
	public boolean checkAndInsertPublishUUID(String uuid) {
		String sql = "select count(1) from t_redeem_uuid where uuid = :uuid";
		MapSqlParameterSource params = new MapSqlParameterSource("uuid", uuid);
		Integer count = jt.queryForObject(sql, params, Integer.class);
		if (count > 0) {
			return false;
		} else {
			params.addValue("received_at", new Date());
			reedemUUIDInsert.execute(params);
			return true;
		}
	}

	@Override
	public int getMaxExposedIdForBatchReleaseTime(Long batchReleaseTime, long batchLength) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("batchReleaseTime", Date.from(Instant.ofEpochMilli(batchReleaseTime)));
		params.addValue("startBatch", Date.from(Instant.ofEpochMilli(batchReleaseTime - batchLength)));
		String sql = "select max(pk_exposed_id) from t_exposed where received_at >= :startBatch and received_at < :batchReleaseTime";
		Integer maxId = jt.queryForObject(sql, params, Integer.class);
		if (maxId == null) {
			return 0;
		} else {
			return maxId;
		}
	}

	@Override
	public List<Exposee> getSortedExposedForBatchReleaseTime(Long batchReleaseTime, long batchLength) {
		String sql = "select pk_exposed_id, key, key_date from t_exposed where received_at >= :startBatch and received_at < :batchReleaseTime order by pk_exposed_id desc";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("batchReleaseTime", Date.from(Instant.ofEpochMilli(batchReleaseTime)));
		params.addValue("startBatch", Date.from(Instant.ofEpochMilli(batchReleaseTime - batchLength)));
		return jt.query(sql, params, new ExposeeRowMapper());
	}

	@Override
	@Transactional(readOnly = false)
	public void cleanDB(int retentionDays) {
		OffsetDateTime retentionTime = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusDays(retentionDays);
		logger.info("Cleanup DB entries before: " + retentionTime);
		MapSqlParameterSource params = new MapSqlParameterSource("retention_time", Date.from(retentionTime.toInstant()));
		String sqlExposed = "delete from t_exposed where received_at < :retention_time";
		jt.update(sqlExposed, params);
		String sqlRedeem = "delete from t_redeem_uuid where received_at < :retention_time";
		jt.update(sqlRedeem, params);
	}
}
