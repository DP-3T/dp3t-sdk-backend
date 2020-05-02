/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.data;

import java.time.*;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.dpppt.backend.sdk.model.Exposee;
import org.dpppt.backend.sdk.model.HealthCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
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
	private final SimpleJdbcInsert signingAuthroizationCodeInsert;
	private final SimpleJdbcInsert blindSignatureIdInsert;

	public JDBCDPPPTDataServiceImpl(String dbType, DataSource dataSource) {
		this.dbType = dbType;
		this.jt = new NamedParameterJdbcTemplate(dataSource);
		this.reedemUUIDInsert = new SimpleJdbcInsert(dataSource).withTableName("t_redeem_uuid")
				.usingGeneratedKeyColumns("pk_redeem_uuid_id");
		this.signingAuthroizationCodeInsert = new SimpleJdbcInsert(dataSource)
				.withTableName("t_signing_authorization_code")
				.usingGeneratedKeyColumns("pk_signing_authorization_code_id");
		this.blindSignatureIdInsert = new SimpleJdbcInsert(dataSource)
				.withTableName("t_blind_signature_id")
				.usingGeneratedKeyColumns("pk_blind_signature_id_id");
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
	public boolean insertSigningAuthorizationCode(byte[] scryptedAuthorizationCode, HealthCondition healthCondition) {
		assert scryptedAuthorizationCode != null && scryptedAuthorizationCode.length > 0;
		assert healthCondition != null;

		try {
			MapSqlParameterSource parameters = new MapSqlParameterSource()
					.addValue("scrypt_code", scryptedAuthorizationCode)
					.addValue("scrypt_code_version", 0)
					.addValue("health_condition", healthCondition.value)
					.addValue("generated_at", LocalDateTime.now());
			int rowsAffected = signingAuthroizationCodeInsert.execute(parameters);

			assert rowsAffected == 1;

			return rowsAffected == 1;
		} catch (DuplicateKeyException e) {
			// Scrypted authorization code collision. Try again.
			return false;
		}
	}

	@Override
	public boolean updateSigningAuthorizationCode(byte[] scryptedAuthorizationCode, HealthCondition healthCondition,
												  LocalDateTime generatedNotLaterThan) {
		assert scryptedAuthorizationCode != null && scryptedAuthorizationCode.length > 0;
		assert healthCondition != null;
		assert generatedNotLaterThan != null;

		MapSqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("redeemed_at", LocalDateTime.now())
				.addValue("scrypt_code", scryptedAuthorizationCode)
				.addValue("health_condition", healthCondition.value)
				.addValue("generated_at_not_later_than", generatedNotLaterThan);
		String sql = "UPDATE t_signing_authorization_code SET redeemed_at = :redeemed_at "
				+ "WHERE scrypt_code = :scrypt_code AND scrypt_code_version = 0 AND health_condition = :health_condition AND redeemed_at is null AND generated_at > :generated_at_not_later_than";
		int rowsAffected = jt.update(sql, parameters);

		assert rowsAffected == 1 || rowsAffected == 0;

		return rowsAffected == 1;
	}

	@Override
	public boolean insertBlindSignatureId(byte[] blindSignatureId) {
		assert blindSignatureId != null && blindSignatureId.length > 0;

		try {
			MapSqlParameterSource parameters = new MapSqlParameterSource()
					.addValue("blind_signature_id", blindSignatureId)
					.addValue("redeemed_at", LocalDateTime.now());
			int rowsAffected = blindSignatureIdInsert.execute(parameters);

			assert rowsAffected == 1;

			return rowsAffected == 1;
		} catch (DuplicateKeyException e) {
			// Blind signature id collision. Start over.
			return false;
		}
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
