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

import org.assertj.core.api.Assertions;
import org.dpppt.backend.sdk.data.PostgresDPPPTDataServiceTest;
import org.dpppt.backend.sdk.data.RedeemDataService;
import org.dpppt.backend.sdk.data.config.DPPPTDataServiceConfig;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.PostgresDataConfig;
import org.dpppt.backend.sdk.model.Exposee;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.sql.DataSource;
import java.sql.*;
import java.time.*;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {PostgresDataConfig.class, FlyWayConfig.class, DPPPTDataServiceConfig.class})
@ActiveProfiles("postgres")
public class PostgresGaenDataServiceTest {

    private static final String APP_SOURCE = "test-app";
    private static final long BATCH_LENGTH = 2 * 60 * 60 * 1000L;
    
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
        insertExposeeWithReceivedAt(receivedAt.toInstant(), key);

		List<GaenKey> sortedExposedForDay = dppptDataService
				.getSortedExposedForBatchReleaseTime(receivedAt.toInstant().toEpochMilli() + Duration.ofMinutes(10).toMillis(), 24 * 60 * 60 * 1000l);
		assertFalse(sortedExposedForDay.isEmpty());

		dppptDataService.cleanDB(Duration.ofDays(21));
		sortedExposedForDay = dppptDataService.getSortedExposedForBatchReleaseTime(receivedAt.toInstant().toEpochMilli() +  Duration.ofMinutes(10).toMillis(),
				24 * 60 * 60 * 1000l);
		assertTrue(sortedExposedForDay.isEmpty());

    }

    @Test
    public void upsert() throws Exception {
        var tmpKey = new GaenKey();
        tmpKey.setRollingStartNumber((int)Duration.ofMillis(Instant.now().minus(Duration.ofDays(1)).toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
        tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
        tmpKey.setRollingPeriod(144);
        tmpKey.setFake(0);
        tmpKey.setTransmissionRiskLevel(0);
        List<GaenKey> keys = List.of(tmpKey);

        dppptDataService.upsertExposees(keys);
        var returnedKeys = dppptDataService.getSortedExposedForBatchReleaseTime(LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),Duration.ofDays(1).toMillis());

        assertEquals(keys.size(), returnedKeys.size());
        assertEquals(keys.get(0).getKeyData(), returnedKeys.get(0).getKeyData());
    }

    @Test
    public void testBatchReleaseTime() throws SQLException {
        Instant receivedAt = LocalDateTime.parse("2014-01-28T00:00:00").toInstant(ZoneOffset.UTC);
        String key = "key555";
        insertExposeeWithReceivedAt(receivedAt, key);

        long batchTime = LocalDateTime.parse("2014-01-28T02:00:00").toInstant(ZoneOffset.UTC).toEpochMilli();
        List<GaenKey> sortedExposedForBatchReleaseTime = dppptDataService.getSortedExposedForBatchReleaseTime(batchTime, BATCH_LENGTH);
        assertEquals(1, sortedExposedForBatchReleaseTime.size());
        GaenKey actual = sortedExposedForBatchReleaseTime.get(0);
        assertEquals(actual.getKeyData(), key);
        int maxExposedIdForBatchReleaseTime = dppptDataService.getMaxExposedIdForBatchReleaseTime(batchTime, BATCH_LENGTH);
        assertEquals(100, maxExposedIdForBatchReleaseTime);
        maxExposedIdForBatchReleaseTime = dppptDataService.getMaxExposedIdForBatchReleaseTime(receivedAt.toEpochMilli(), PostgresDPPPTDataServiceTest.BATCH_LENGTH);
        assertEquals(0, maxExposedIdForBatchReleaseTime);
    }


    private void insertExposeeWithReceivedAt(Instant receivedAt, String key) throws SQLException {
        Connection connection = dataSource.getConnection();
        String sql = "into t_gaen_exposed (pk_exposed_id, key, received_at, rolling_start_number, rolling_period, transmission_risk_level) values (100, ?, ?, ?, 144, 0)";
        PreparedStatement preparedStatement = connection.prepareStatement("insert " + sql);
        preparedStatement.setString(1, key);
        preparedStatement.setTimestamp(2, new Timestamp(receivedAt.toEpochMilli()));
        preparedStatement.setInt(3, (int)Duration.ofMillis(receivedAt.toEpochMilli()).dividedBy(Duration.ofMinutes(10)));
        preparedStatement.execute();
    }

    @NotNull
    private Exposee createExposee(String key, String keyDate) {
        Exposee exposee = new Exposee();
        exposee.setKey(key);
        exposee.setKeyDate(LocalDate.parse("2014-01-28").atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());
        return exposee;
    }

    private long getExposeeCount() throws SQLException {
        try (
                final Connection connection = dataSource.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement("select count(*) from t_exposed");
                final ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    protected void executeSQL(String sql) throws SQLException {
        try (
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ) {
            preparedStatement.execute();
        }
    }
}
