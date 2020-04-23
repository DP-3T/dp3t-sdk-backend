/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.data;

import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.dpppt.backend.sdk.model.Exposee;
import org.joda.time.DateTime;
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
            sql = "insert into t_exposed (key, onset, app_source) values (:key, to_date(:onset, 'yyyy-MM-dd'), :app_source)"
                + " on conflict on constraint key do nothing";
        } else {
            sql = "merge into t_exposed using (values(cast(:key as varchar(10000)), cast(:onset as date), cast(:app_source as varchar(50))))"
                + " as vals(key, onset, app_source) on t_exposed.key = vals.key"
                + " when not matched then insert (key, onset, app_source) values (vals.key, vals.onset, vals.app_source)";
        }
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("key", exposee.getKey());
        params.addValue("app_source", appSource);
        params.addValue("onset", exposee.getOnset());
        jt.update(sql, params);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exposee> getSortedExposedForDay(DateTime day) {
        DateTime dayMidnight = day.withTimeAtStartOfDay();
        String sql = "select pk_exposed_id, key, to_char(onset, 'yyyy-MM-dd') as onset_string from t_exposed where received_at >= :dayMidnight and received_at < :nextDayMidnight order by pk_exposed_id desc";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("dayMidnight", dayMidnight.toDate());
        params.addValue("nextDayMidnight", dayMidnight.plusDays(1).toDate());
        return jt.query(sql, params, new ExposeeRowMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getMaxExposedIdForDay(DateTime day) {
        DateTime dayMidnight = day.withTimeAtStartOfDay();
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("dayMidnight", dayMidnight.toDate());
        params.addValue("nextDayMidnight", dayMidnight.plusDays(1).toDate());
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
        params.addValue("batchReleaseTime", new DateTime(batchReleaseTime).toDate());
        params.addValue("startBatch", new DateTime(batchReleaseTime - batchLength).toDate());
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
        String sql = "select pk_exposed_id, key, to_char(onset, 'yyyy-MM-dd') as onset_string from t_exposed where received_at >= :startBatch and received_at < :batchReleaseTime order by pk_exposed_id desc";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("batchReleaseTime", new DateTime(batchReleaseTime).toDate());
        params.addValue("startBatch", new DateTime(batchReleaseTime - batchLength).toDate());
        return jt.query(sql, params, new ExposeeRowMapper());
    }
}
