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
import java.util.List;
import javax.sql.DataSource;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenKeyWithOrigin;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

public class JDBCGAENDataServiceImpl implements GAENDataService {

  private static final Logger logger = LoggerFactory.getLogger(JDBCGAENDataServiceImpl.class);

  private static final String PGSQL = "pgsql";
  private final String dbType;
  private final NamedParameterJdbcTemplate jt;
  private final Duration releaseBucketDuration;
  // Time skew means the duration for how long a key still is valid __after__ it has expired (e.g 2h
  // for now
  // https://developer.apple.com/documentation/exposurenotification/setting_up_a_key_server?language=objc)
  private final Duration timeSkew;

  // the origin country is used for the "default" visited country for all insertions that do not
  // provide the visited countries for the key, so all v1 and non-international v2 inserted keys.
  // the origin country is also the default for returning keys.
  private final String originCountry;

  public JDBCGAENDataServiceImpl(
      String dbType,
      DataSource dataSource,
      Duration releaseBucketDuration,
      Duration timeSkew,
      String originCountry) {
    this.dbType = dbType;
    this.jt = new NamedParameterJdbcTemplate(dataSource);
    this.releaseBucketDuration = releaseBucketDuration;
    this.timeSkew = timeSkew;
    this.originCountry = originCountry;
  }

  @Override
  @Transactional(readOnly = false)
  public void upsertExposeeFromInterops(GaenKey gaenKey, UTCInstant now, String origin) {
    internalUpsertKey(gaenKey, now, origin, true);
  }

  @Override
  @Transactional(readOnly = false)
  public void upsertExposees(
      List<GaenKey> gaenKeys, UTCInstant now, boolean withFederationGateway) {
    upsertExposeesDelayed(gaenKeys, null, now, withFederationGateway);
  }

  @Override
  @Transactional(readOnly = false)
  public void upsertExposeesDelayed(
      List<GaenKey> gaenKeys,
      UTCInstant delayedReceivedAt,
      UTCInstant now,
      boolean withFederationGateway) {
    // Calculate the `receivedAt` just at the end of the current releaseBucket.
    var receivedAt =
        delayedReceivedAt == null
            ? now.roundToNextBucket(releaseBucketDuration).minus(Duration.ofMillis(1))
            : delayedReceivedAt;

    for (var gaenKey : gaenKeys) {
      internalUpsertKey(gaenKey, receivedAt, this.originCountry, withFederationGateway);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<GaenKey> getSortedExposedForKeyDate(
      UTCInstant keyDate, UTCInstant publishedAfter, UTCInstant publishedUntil, UTCInstant now) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("rollingPeriodStartNumberStart", keyDate.get10MinutesSince1970());
    params.addValue("rollingPeriodStartNumberEnd", keyDate.plusDays(1).get10MinutesSince1970());
    params.addValue("publishedUntil", publishedUntil.getDate());

    String sql =
        "select pk_exposed_id, key, rolling_start_number, rolling_period"
            + " from t_gaen_exposed where rolling_start_number >= :rollingPeriodStartNumberStart"
            + " and rolling_start_number < :rollingPeriodStartNumberEnd and received_at <"
            + " :publishedUntil";
    // we need to subtract the time skew since we want to release it iff rolling_start_number +
    // rolling_period + timeSkew < NOW
    // note though that since we use `<` instead of `<=` a key which is valid until 24:00 will be
    // accepted until 02:00 (by the clients, so we MUST NOT release it before 02:00), but 02:00 lies
    // in the bucket of 04:00. So the key will be released
    // earliest 04:00.
    params.addValue(
        "maxAllowedStartNumber",
        now.roundToBucketStart(releaseBucketDuration).minus(timeSkew).get10MinutesSince1970());
    sql += " and rolling_start_number + rolling_period < :maxAllowedStartNumber";

    // note that received_at is always rounded to `next_bucket` - 1ms to difuse actual upload time
    if (publishedAfter != null) {
      params.addValue("publishedAfter", publishedAfter.getDate());
      sql += " and received_at >= :publishedAfter";
    }

    sql += " order by pk_exposed_id desc";

    return jt.query(sql, params, new GaenKeyRowMapper());
  }

  @Override
  @Transactional(readOnly = true)
  public List<GaenKey> getSortedExposedSince(
      UTCInstant keysSince, UTCInstant now, boolean withFederationGateway) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("since", keysSince.getDate());
    params.addValue("maxBucket", now.roundToBucketStart(releaseBucketDuration).getDate());
    params.addValue("timeSkewSeconds", timeSkew.toSeconds());

    // if with federation, do not check origin.
    String originCondition = "";
    if (!withFederationGateway) {
      params.addValue("origin", originCountry);
      originCondition = " keys.origin = :origin and ";
    }

    // Select keys since the given date. We need to make sure, only keys are returned
    // that are allowed to be published.
    // For this, we calculate the expiry for each key in a sub query. The expiry is then used for
    // the where clause:
    // - if expiry <= received_at: the key was ready to publish when we received it. Release this
    // key, if received_at in [since, maxBucket)
    // - if expiry > received_at: we have to wait until expiry till we can release this key. This
    // means we only release the key if expiry in [since, maxBucket)
    // This problem arises, because we only want key with received_at after since, but we need to
    // ensure that we relase ALL keys meaning keys which were still valid when they were received

    // we need to add the time skew to calculate the expiry timestamp of a key:
    // TO_TIMESTAMP((rolling_start_number + rolling_period) * 10 * 60 + :timeSkewSeconds

    String sql =
        "select distinct keys.pk_exposed_id, keys.key, keys.rolling_start_number,"
            + " keys.rolling_period from (select pk_exposed_id, key, rolling_start_number,"
            + " rolling_period, received_at, origin, "
            + getSQLExpressionForExpiry()
            + " as expiry from t_gaen_exposed)"
            + " as keys where"
            + originCondition
            + "((keys.received_at >= :since AND"
            + " keys.received_at < :maxBucket AND keys.expiry <= keys.received_at) OR (keys.expiry"
            + " >= :since AND keys.expiry < :maxBucket AND keys.expiry > keys.received_at))";

    sql += " order by keys.pk_exposed_id desc";

    return jt.query(sql, params, new GaenKeyRowMapper());
  }

  @Override
  @Transactional(readOnly = true)
  public List<GaenKeyWithOrigin> getSortedExposedSinceWithOriginFromOrigin(
      UTCInstant keysSince, UTCInstant now) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("since", keysSince.getDate());
    params.addValue("maxBucket", now.roundToBucketStart(releaseBucketDuration).getDate());
    params.addValue("timeSkewSeconds", timeSkew.toSeconds());
    params.addValue("origin", originCountry);

    // Select keys since the given date. We need to make sure, only keys are returned
    // that are allowed to be published and from our own origin country.
    // For this, we calculate the expiry for each key in a sub query. The expiry is then used for
    // the where clause:
    // - if expiry <= received_at: the key was ready to publish when we received it. Release this
    // key, if received_at in [since, maxBucket)
    // - if expiry > received_at: we have to wait until expiry till we can release this key. This
    // means we only release the key if expiry in [since, maxBucket)
    // This problem arises, because we only want key with received_at after since, but we need to
    // ensure that we relase ALL keys meaning keys which were still valid when they were received

    // we need to add the time skew to calculate the expiry timestamp of a key:
    // TO_TIMESTAMP((rolling_start_number + rolling_period) * 10 * 60 + :timeSkewSeconds

    String sql =
        "select keys.pk_exposed_id, keys.key, keys.rolling_start_number, keys.origin, "
            + " keys.rolling_period from (select pk_exposed_id, key, rolling_start_number,"
            + " rolling_period, received_at, origin, "
            + getSQLExpressionForExpiry()
            + " as expiry from t_gaen_exposed)"
            + " as keys where keys.origin = :origin and ((keys.received_at >= :since AND"
            + " keys.received_at < :maxBucket AND keys.expiry <= keys.received_at) OR (keys.expiry"
            + " >= :since AND keys.expiry < :maxBucket AND keys.expiry > keys.received_at))";

    sql += " order by keys.pk_exposed_id desc";

    return jt.query(sql, params, new GaenKeyWithOriginRowMapper());
  }

  private String getSQLExpressionForExpiry() {
    if (this.dbType.equals(PGSQL)) {
      return "TO_TIMESTAMP((rolling_start_number + rolling_period) * 10 * 60 +"
          + " :timeSkewSeconds)";
    } else {
      return "TIMESTAMP_WITH_ZONE((rolling_start_number + rolling_period) * 10 * 60 +"
          + " :timeSkewSeconds)";
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void cleanDB(Duration retentionPeriod) {
    var retentionTime = UTCInstant.now().minus(retentionPeriod);
    logger.info("Cleanup DB entries before: " + retentionTime);
    MapSqlParameterSource params =
        new MapSqlParameterSource("retention_time", retentionTime.getDate());
    String sqlExposed = "delete from t_gaen_exposed where received_at < :retention_time";
    jt.update(sqlExposed, params);
  }

  private void internalUpsertKey(
      GaenKey gaenKey, UTCInstant receivedAt, String origin, boolean withFederationGateway) {
    String sqlKey = null;
    if (dbType.equals(PGSQL)) {
      sqlKey =
          "insert into t_gaen_exposed (key, rolling_start_number, rolling_period, received_at,"
              + " origin, share_with_federation_gateway) values (:key, :rolling_start_number,"
              + " :rolling_period, :received_at, :origin, :share_with_federation_gateway) on"
              + " conflict on constraint gaen_exposed_key do nothing";
    } else {
      sqlKey =
          "merge into t_gaen_exposed using (values(cast(:key as varchar(24)),"
              + " :rolling_start_number, :rolling_period, :received_at, cast(:origin as"
              + " varchar(10)), :share_with_federation_gateway)) as vals(key,"
              + " rolling_start_number, rolling_period, received_at, origin,"
              + " share_with_federation_gateway) on t_gaen_exposed.key = vals.key when not matched"
              + " then insert (key, rolling_start_number, rolling_period, received_at, origin,"
              + " share_with_federation_gateway) values (vals.key, vals.rolling_start_number,"
              + " vals.rolling_period, vals.received_at, vals.origin,"
              + " vals.share_with_federation_gateway)";
    }

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("key", gaenKey.getKeyData());
    params.addValue("rolling_start_number", gaenKey.getRollingStartNumber());
    params.addValue("rolling_period", gaenKey.getRollingPeriod());
    params.addValue("received_at", receivedAt.getDate());
    params.addValue("origin", origin);
    params.addValue("share_with_federation_gateway", withFederationGateway);
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jt.update(sqlKey, params, keyHolder);
  }
}
