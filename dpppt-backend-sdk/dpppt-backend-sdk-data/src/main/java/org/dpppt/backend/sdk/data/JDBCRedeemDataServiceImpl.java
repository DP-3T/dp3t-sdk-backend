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

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

public class JDBCRedeemDataServiceImpl implements RedeemDataService {

	private static final Logger logger = LoggerFactory.getLogger(JDBCRedeemDataServiceImpl.class);

	private final NamedParameterJdbcTemplate jt;
	private final SimpleJdbcInsert reedemUUIDInsert;
	private Clock currentClock = Clock.systemUTC();

	public JDBCRedeemDataServiceImpl(DataSource dataSource) {
		this.jt = new NamedParameterJdbcTemplate(dataSource);
		this.reedemUUIDInsert = new SimpleJdbcInsert(dataSource).withTableName("t_redeem_uuid")
				.usingGeneratedKeyColumns("pk_redeem_uuid_id");
	}

	@Override
	@Transactional(readOnly = false)
	public boolean checkAndInsertPublishUUID(String uuid) {
		String sql = "select count(1) from t_redeem_uuid where uuid = :uuid";
		MapSqlParameterSource params = new MapSqlParameterSource("uuid", uuid);
		Integer count = jt.queryForObject(sql, params, Integer.class);
		if (count > 0) {
			return false;
		} else {
			// set the received_at to the next day, with no time information
			// it will stay longer in the DB but we mitigate the risk that the JWT
			// can be used twice (c.f. testTokensArentDeletedBeforeExpire). 
			long startOfDay = LocalDate.now(currentClock).atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant().toEpochMilli();
			params.addValue("received_at", new Date(startOfDay));
			reedemUUIDInsert.execute(params);
			return true;
		}
	}

	@Override
	@Transactional(readOnly = false)
	public void cleanDB(Duration retentionPeriod) {
		OffsetDateTime retentionTime = OffsetDateTime.now(currentClock).minus(retentionPeriod);
		logger.info("Cleanup DB entries before: " + retentionTime);
		MapSqlParameterSource params = new MapSqlParameterSource("retention_time", Date.from(retentionTime.toInstant()));
		String sqlRedeem = "delete from t_redeem_uuid where received_at < :retention_time";
		jt.update(sqlRedeem, params);
	}
}
