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

import org.assertj.core.api.Assertions;
import org.dpppt.backend.sdk.data.config.DPPPTDataServiceConfig;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.PostgresDataConfig;
import org.dpppt.backend.sdk.model.Exposee;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {PostgresDataConfig.class, FlyWayConfig.class, DPPPTDataServiceConfig.class})
@ActiveProfiles("postgres")
public class PostgresDPPPTDataServiceTest {

    private static final String APP_SOURCE = "test-app";
    private static final long BATCH_LENGTH = 2 * 60 * 60 * 1000L;
    
    @Autowired
    private DPPPTDataService dppptDataService;

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
    public void shouldAddAnExposee() throws SQLException {
        // GIVEN
        final long exposeeCountBefore = getExposeeCount();

        Exposee exposee = createExposee("key1", "2014-01-28");

        // WHEN
        dppptDataService.upsertExposee(exposee, "test-app");

        // THEN
        final long exposeeCountAfter = getExposeeCount();
        Assertions.assertThat(exposeeCountAfter).isEqualTo(exposeeCountBefore + 1);

        try (
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement("select * from t_exposed t where t.key = 'key1'");
            final ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();

            Assertions.assertThat(resultSet.getInt("pk_exposed_id")).isPositive();
            Assertions.assertThat(resultSet.getString("key")).isEqualTo("key1");
            Assertions.assertThat(resultSet.getString("received_at")).isNotNull();
            Assertions.assertThat(resultSet.getString("app_source")).isEqualTo(APP_SOURCE);
            Assertions.assertThat(resultSet.getDate("key_date")).isEqualTo(Date.valueOf(LocalDate.of(2014, 1, 28)));
        }
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

		List<Exposee> sortedExposedForDay = dppptDataService
				.getSortedExposedForBatchReleaseTime(now.toInstant().toEpochMilli(), 1 * 60 * 60 * 1000l);
		assertFalse(sortedExposedForDay.isEmpty());

		dppptDataService.cleanDB(Duration.ofDays(21));
		sortedExposedForDay = dppptDataService.getSortedExposedForBatchReleaseTime(now.toInstant().toEpochMilli(),
				1 * 60 * 60 * 1000l);
		assertTrue(sortedExposedForDay.isEmpty());

    }

    @Test
    public void testBatchReleaseTime() throws SQLException {
        Instant receivedAt = LocalDateTime.parse("2014-01-28T00:00:00").toInstant(ZoneOffset.UTC);
        String key = "key555";
        insertExposeeWithReceivedAt(receivedAt, key);

        long batchTime = LocalDateTime.parse("2014-01-28T02:00:00").toInstant(ZoneOffset.UTC).toEpochMilli();
        List<Exposee> sortedExposedForBatchReleaseTime = dppptDataService.getSortedExposedForBatchReleaseTime(batchTime, BATCH_LENGTH);
        assertEquals(1, sortedExposedForBatchReleaseTime.size());
        Exposee actual = sortedExposedForBatchReleaseTime.get(0);
        assertEquals(actual.getKey(), key);
        int maxExposedIdForBatchReleaseTime = dppptDataService.getMaxExposedIdForBatchReleaseTime(batchTime, BATCH_LENGTH);
        assertEquals(1, maxExposedIdForBatchReleaseTime);
        maxExposedIdForBatchReleaseTime = dppptDataService.getMaxExposedIdForBatchReleaseTime(receivedAt.toEpochMilli(), PostgresDPPPTDataServiceTest.BATCH_LENGTH);
        assertEquals(0, maxExposedIdForBatchReleaseTime);
    }


    private void insertExposeeWithReceivedAt(Instant receivedAt, String key) throws SQLException {
        Connection connection = dataSource.getConnection();
        String sql = "into t_exposed (pk_exposed_id, key, received_at, key_date, app_source) values (1, ?, ?, now(), 'appsource')";
        PreparedStatement preparedStatement = connection.prepareStatement("insert " + sql);
        preparedStatement.setString(1, key);
        preparedStatement.setTimestamp(2, new Timestamp(receivedAt.toEpochMilli()));
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
