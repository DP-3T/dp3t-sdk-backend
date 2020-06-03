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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import javax.sql.DataSource;

import org.dpppt.backend.sdk.data.RedeemDataService;
import org.dpppt.backend.sdk.data.config.DPPPTDataServiceConfig;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.PostgresDataConfig;
import org.dpppt.backend.sdk.model.Exposee;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { PostgresDataConfig.class,
        FlyWayConfig.class, DPPPTDataServiceConfig.class })
@ActiveProfiles("postgres")
public class PostgresGaenDataServiceTest {

    private static final String APP_SOURCE = "test-app";
    private static final Duration BATCH_LENGTH = Duration.ofHours(2);

    @Autowired
    private GAENDataService dppptDataService;

    @Autowired
    private RedeemDataService redeemDataService;

    @Autowired
    private DataSource dataSource;

    @After
    public void tearDown() throws SQLException {
        executeSQL("truncate table t_exposed");
        executeSQL("truncate table t_redeem_uuid");
    }

    @Test
    public void testRedeemUUID() {
        boolean actual = redeemDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
        assertTrue(actual);
        actual = redeemDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
        assertFalse(actual);
        actual = redeemDataService.checkAndInsertPublishUUID("1c444adb-0924-4dc4-a7eb-1f52aa6b9575");
        assertTrue(actual);
    }

    @Test
    public void cleanup() throws SQLException {
        OffsetDateTime now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime receivedAt = now.minusDays(21);
        Connection connection = dataSource.getConnection();
        String key = "someKey";
        insertExposeeWithReceivedAtAndKeyDate(receivedAt.toInstant(), receivedAt.minusDays(1).toInstant(), key);

        List<GaenKey> sortedExposedForDay = dppptDataService.getSortedExposedForKeyDate(
                receivedAt.minusDays(1).toInstant().toEpochMilli(), null, now.toInstant().toEpochMilli());

        assertFalse(sortedExposedForDay.isEmpty());

        dppptDataService.cleanDB(Duration.ofDays(21));
        sortedExposedForDay = dppptDataService.getSortedExposedForKeyDate(
                receivedAt.minusDays(1).toInstant().toEpochMilli(), null, now.toInstant().toEpochMilli());

        assertTrue(sortedExposedForDay.isEmpty());

    }

    @Test
    public void upsert() throws Exception {
        var tmpKey = new GaenKey();
        tmpKey.setRollingStartNumber((int) Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli())
                .dividedBy(Duration.ofMinutes(10)));
        tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
        tmpKey.setRollingPeriod(144);
        tmpKey.setFake(0);
        tmpKey.setTransmissionRiskLevel(0);
        List<GaenKey> keys = List.of(tmpKey);

        dppptDataService.upsertExposees(keys);

        long now = System.currentTimeMillis();
        // calculate exposed until bucket, but get bucket in the future, as keys have
        // been inserted with timestamp now.
        long publishedUntil = now - (now % BATCH_LENGTH.toMillis()) + BATCH_LENGTH.toMillis();

        var returnedKeys = dppptDataService.getSortedExposedForKeyDate(
                Instant.now().minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS).toEpochMilli(), null,
                publishedUntil);

        assertEquals(keys.size(), returnedKeys.size());
        assertEquals(keys.get(0).getKeyData(), returnedKeys.get(0).getKeyData());
    }

    @Test
    public void testBatchReleaseTime() throws SQLException {
        Instant receivedAt = LocalDateTime.parse("2014-01-28T00:00:00").toInstant(ZoneOffset.UTC);
        String key = "key555";
        insertExposeeWithReceivedAtAndKeyDate(receivedAt, receivedAt.minus(Duration.ofDays(2)), key);

        long batchTime = LocalDateTime.parse("2014-01-28T02:00:00").toInstant(ZoneOffset.UTC).toEpochMilli();

        var returnedKeys = dppptDataService
                .getSortedExposedForKeyDate(receivedAt.minus(Duration.ofDays(2)).toEpochMilli(), null, batchTime);

        assertEquals(1, returnedKeys.size());
        GaenKey actual = returnedKeys.get(0);
        assertEquals(actual.getKeyData(), key);

        int maxExposedIdForBatchReleaseTime = dppptDataService
                .getMaxExposedIdForKeyDate(receivedAt.minus(Duration.ofDays(2)).toEpochMilli(), null, batchTime);
        assertEquals(100, maxExposedIdForBatchReleaseTime);

        returnedKeys = dppptDataService.getSortedExposedForKeyDate(receivedAt.minus(Duration.ofDays(2)).toEpochMilli(),
                batchTime, batchTime + 2 * 60 * 60 * 1000l);
        assertEquals(0, returnedKeys.size());
    }

    private void insertExposeeWithReceivedAt(Instant receivedAt, String key) throws SQLException {
        Connection connection = dataSource.getConnection();
        String sql = "into t_gaen_exposed (pk_exposed_id, key, received_at, rolling_start_number, rolling_period, transmission_risk_level) values (100, ?, ?, ?, 144, 0)";
        PreparedStatement preparedStatement = connection.prepareStatement("insert " + sql);
        preparedStatement.setString(1, key);
        preparedStatement.setTimestamp(2, new Timestamp(receivedAt.toEpochMilli()));
        preparedStatement.setInt(3,
                (int) Duration.ofMillis(receivedAt.toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
        preparedStatement.execute();
    }

    private void insertExposeeWithReceivedAtAndKeyDate(Instant receivedAt, Instant keyDate, String key)
            throws SQLException {
        Connection connection = dataSource.getConnection();
        String sql = "into t_gaen_exposed (pk_exposed_id, key, received_at, rolling_start_number, rolling_period, transmission_risk_level) values (100, ?, ?, ?, 144, 0)";
        PreparedStatement preparedStatement = connection.prepareStatement("insert " + sql);
        preparedStatement.setString(1, key);
        preparedStatement.setTimestamp(2, new Timestamp(receivedAt.toEpochMilli()));
        preparedStatement.setInt(3, (int) GaenUnit.TenMinutes.between(Instant.ofEpochMilli(0), keyDate));
        preparedStatement.execute();
    }

    @NotNull
    private Exposee createExposee(String key, String keyDate) {
        Exposee exposee = new Exposee();
        exposee.setKey(key);
        exposee.setKeyDate(
                LocalDate.parse("2014-01-28").atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());
        return exposee;
    }

    private long getExposeeCount() throws SQLException {
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement preparedStatement = connection
                        .prepareStatement("select count(*) from t_exposed");
                final ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    protected void executeSQL(String sql) throws SQLException {
        try (final Connection connection = dataSource.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
            preparedStatement.execute();
        }
    }
}
