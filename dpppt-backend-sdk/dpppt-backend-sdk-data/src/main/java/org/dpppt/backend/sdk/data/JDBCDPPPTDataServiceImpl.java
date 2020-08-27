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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.dpppt.backend.sdk.model.Exposee;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class JDBCDPPPTDataServiceImpl implements DPPPTDataService {

  private static final Logger logger = LoggerFactory.getLogger(JDBCDPPPTDataServiceImpl.class);
  private static final String PGSQL = "pgsql";
  private final String dbType;
  private final NamedParameterJdbcTemplate jt;

  public JDBCDPPPTDataServiceImpl(String dbType, DataSource dataSource) {
    this.dbType = dbType;
    this.jt = new NamedParameterJdbcTemplate(dataSource);
  }

  @Override
  @Transactional(readOnly = false)
  public void upsertExposee(Exposee exposee, String appSource) {
    String sql = null;
    if (dbType.equals(PGSQL)) {
      sql =
          "insert into t_exposed (key, key_date, app_source) values (:key, :key_date, :app_source)"
              + " on conflict on constraint key do nothing";
    } else {
      sql =
          "merge into t_exposed using (values(cast(:key as varchar(10000)), cast(:key_date as"
              + " date), cast(:app_source as varchar(50)))) as vals(key, key_date, app_source) on"
              + " t_exposed.key = vals.key when not matched then insert (key, key_date,"
              + " app_source) values (vals.key, vals.key_date, vals.app_source)";
    }
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("key", exposee.getKey());
    params.addValue("app_source", appSource);
    params.addValue("key_date", UTCInstant.ofEpochMillis(exposee.getKeyDate()).getDate());
    jt.update(sql, params);
  }

  @Override
  @Transactional(readOnly = false)
  public void upsertExposees(List<Exposee> exposees, String appSource) {
    String sql = null;
    if (dbType.equals(PGSQL)) {
      sql =
          "insert into t_exposed (key, key_date, app_source) values (:key, :key_date, :app_source)"
              + " on conflict on constraint key do nothing";
    } else {
      sql =
          "merge into t_exposed using (values(cast(:key as varchar(10000)), cast(:key_date as"
              + " date), cast(:app_source as varchar(50)))) as vals(key, key_date, app_source) on"
              + " t_exposed.key = vals.key when not matched then insert (key, key_date,"
              + " app_source) values (vals.key, vals.key_date, vals.app_source)";
    }
    var parameterList = new ArrayList<MapSqlParameterSource>();
    for (var exposee : exposees) {
      MapSqlParameterSource params = new MapSqlParameterSource();
      params.addValue("key", exposee.getKey());
      params.addValue("app_source", appSource);
      params.addValue("key_date", UTCInstant.ofEpochMillis(exposee.getKeyDate()).getDate());
      parameterList.add(params);
    }
    jt.batchUpdate(sql, parameterList.toArray(new MapSqlParameterSource[0]));
  }

  @Override
  @Transactional(readOnly = true)
  public int getMaxExposedIdForBatchReleaseTime(long batchReleaseTime, long releaseBucketDuration) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("batchReleaseTime", UTCInstant.ofEpochMillis(batchReleaseTime).getDate());
    params.addValue(
        "startBatch", UTCInstant.ofEpochMillis(batchReleaseTime - releaseBucketDuration).getDate());
    String sql =
        "select max(pk_exposed_id) from t_exposed where received_at >= :startBatch and received_at"
            + " < :batchReleaseTime";
    Integer maxId = jt.queryForObject(sql, params, Integer.class);
    if (maxId == null) {
      return 0;
    } else {
      return maxId;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<Exposee> getSortedExposedForBatchReleaseTime(
      long batchReleaseTime, long releaseBucketDuration) {
    String sql =
        "select pk_exposed_id, key, key_date from t_exposed where received_at >= :startBatch and"
            + " received_at < :batchReleaseTime order by pk_exposed_id desc";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("batchReleaseTime", UTCInstant.ofEpochMillis(batchReleaseTime).getDate());
    params.addValue(
        "startBatch", UTCInstant.ofEpochMillis(batchReleaseTime - releaseBucketDuration).getDate());
    return jt.query(sql, params, new ExposeeRowMapper());
  }

  @Override
  @Transactional(readOnly = false)
  public void cleanDB(Duration retentionPeriod) {
    var retentionTime = UTCInstant.now().minus(retentionPeriod);
    logger.info("Cleanup DB entries before: " + retentionTime);
    MapSqlParameterSource params =
        new MapSqlParameterSource("retention_time", retentionTime.getDate());
    String sqlExposed = "delete from t_exposed where received_at < :retention_time";
    jt.update(sqlExposed, params);
  }
}
