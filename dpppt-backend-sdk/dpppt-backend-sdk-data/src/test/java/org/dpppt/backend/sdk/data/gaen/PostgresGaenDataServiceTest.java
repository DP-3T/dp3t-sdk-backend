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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.sql.DataSource;
import org.dpppt.backend.sdk.data.RedeemDataService;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.GaenDataServiceConfig;
import org.dpppt.backend.sdk.data.config.PostgresDataConfig;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    loader = AnnotationConfigContextLoader.class,
    classes = {PostgresDataConfig.class, FlyWayConfig.class, GaenDataServiceConfig.class})
@ActiveProfiles("postgres")
@TestPropertySource(properties = {"ws.gaen.randomkeysenabled=true"})
public class PostgresGaenDataServiceTest {

  private static final String APP_SOURCE = "test-app";
  private static final Duration BATCH_LENGTH = Duration.ofHours(2);

  @Autowired private GaenDataService gaenDataService;
  @Autowired private FakeKeyService fakeKeyService;

  @Autowired private RedeemDataService redeemDataService;

  @Autowired private DataSource dataSource;

  static {
    System.setProperty("ws.origin.country", "CH");
  }

  @After
  public void tearDown() throws SQLException {
    executeSQL("truncate table t_exposed");
    executeSQL("truncate table t_redeem_uuid");
  }

  @Test
  public void testFakeKeyContainsKeysForLast21Days() throws Exception {
    Clock threeOClock = Clock.fixed(UTCInstant.today().plusHours(4).getInstant(), ZoneOffset.UTC);
    try (var lockedClock = UTCInstant.setClock(threeOClock)) {
      var today = UTCInstant.today();
      var now = UTCInstant.now();
      var noKeyAtThisDate = today.minusDays(22);
      var keysUntilToday = today.minusDays(21);

      var keys = new ArrayList<GaenKey>();
      var emptyList =
          fakeKeyService.fillUpKeys(keys, UTCInstant.midnight1970(), noKeyAtThisDate, now);
      assertEquals(0, emptyList.size());
      do {
        keys.clear();
        var list = fakeKeyService.fillUpKeys(keys, UTCInstant.midnight1970(), keysUntilToday, now);

        assertEquals(10, list.size());
        list = fakeKeyService.fillUpKeys(keys, UTCInstant.now().plusHours(3), keysUntilToday, now);
        assertEquals(10, list.size());
        keysUntilToday = keysUntilToday.plusDays(1);
      } while (keysUntilToday.isBeforeDateOf(today));

      keys.clear();
      emptyList = fakeKeyService.fillUpKeys(keys, UTCInstant.midnight1970(), noKeyAtThisDate, now);
      assertEquals(0, emptyList.size());
    }
  }

  @Test
  public void testRedeemUUID() {
    boolean actual =
        redeemDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
    assertTrue(actual);
    actual = redeemDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
    assertFalse(actual);
    actual = redeemDataService.checkAndInsertPublishUUID("1c444adb-0924-4dc4-a7eb-1f52aa6b9575");
    assertTrue(actual);
  }

  @Test
  public void testTokensArentDeletedBeforeExpire() throws Exception {
    var localDateNow = UTCInstant.today();
    Clock twoMinutesToMidnight =
        Clock.fixed(localDateNow.plusDays(1).minusMinutes(2).getInstant(), ZoneOffset.UTC);
    Clock twoMinutesAfterMidnight =
        Clock.fixed(localDateNow.plusDays(1).plusMinutes(2).getInstant(), ZoneOffset.UTC);
    Clock nextDay =
        Clock.fixed(localDateNow.plusDays(2).plusMinutes(2).getInstant(), ZoneOffset.UTC);

    try (var lockedClock = UTCInstant.setClock(twoMinutesToMidnight)) {
      boolean actual =
          redeemDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
      assertTrue(actual);
    }

    // token is still valid for 1 minute
    try (var lockedClock = UTCInstant.setClock(twoMinutesAfterMidnight)) {
      redeemDataService.cleanDB(Duration.ofDays(1));

      boolean actual =
          redeemDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
      assertFalse(actual);
    }

    try (var lockedClock = UTCInstant.setClock(nextDay)) {
      redeemDataService.cleanDB(Duration.ofDays(1));

      boolean actual =
          redeemDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
      assertTrue(actual);
    }
  }

  @Test
  public void cleanup() throws SQLException {
    var now = UTCInstant.now();
    var receivedAt = now.minusDays(21);
    Connection connection = dataSource.getConnection();
    String key = "someKey";
    insertExposeeWithReceivedAtAndKeyDate(
        receivedAt.getInstant(), receivedAt.minusDays(1).getInstant(), key);

    List<GaenKey> sortedExposedForDay =
        gaenDataService.getSortedExposedForKeyDate(
            receivedAt.minusDays(1), UTCInstant.midnight1970(), now, now, true);

    assertFalse(sortedExposedForDay.isEmpty());

    gaenDataService.cleanDB(Duration.ofDays(21));
    sortedExposedForDay =
        gaenDataService.getSortedExposedForKeyDate(
            receivedAt.minusDays(1), UTCInstant.midnight1970(), now, now, true);

    assertTrue(sortedExposedForDay.isEmpty());
  }

  @Test
  public void upsert() throws Exception {
    var tmpKey = new GaenKey();
    tmpKey.setRollingStartNumber(
        (int) UTCInstant.today().minus(Duration.ofDays(1)).get10MinutesSince1970());
    tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    tmpKey.setRollingPeriod(144);
    tmpKey.setFake(0);
    List<GaenKey> keys = List.of(tmpKey);

    gaenDataService.upsertExposees(keys, UTCInstant.now(), false);

    var now = UTCInstant.now();
    // calculate exposed until bucket, but get bucket in the future, as keys have
    // been inserted with timestamp now.
    UTCInstant publishedUntil = now.roundToNextBucket(BATCH_LENGTH);

    var returnedKeys =
        gaenDataService.getSortedExposedForKeyDate(
            UTCInstant.today().minus(Duration.ofDays(1)),
            UTCInstant.midnight1970(),
            publishedUntil,
            now,
            true);

    assertEquals(keys.size(), returnedKeys.size());
    assertEquals(keys.get(0).getKeyData(), returnedKeys.get(0).getKeyData());
  }

  @Test
  public void testBatchReleaseTime() throws SQLException {
    var receivedAt = UTCInstant.parseDateTime("2014-01-28T00:00:00");
    var now = UTCInstant.now();
    String key = "key555";
    insertExposeeWithReceivedAtAndKeyDate(
        receivedAt.getInstant(), receivedAt.minus(Duration.ofDays(2)).getInstant(), key);

    var batchTime = UTCInstant.parseDateTime("2014-01-28T02:00:00");

    var returnedKeys =
        gaenDataService.getSortedExposedForKeyDate(
            receivedAt.minus(Duration.ofDays(2)), UTCInstant.midnight1970(), batchTime, now, true);

    assertEquals(1, returnedKeys.size());
    GaenKey actual = returnedKeys.get(0);
    assertEquals(actual.getKeyData(), key);

    returnedKeys =
        gaenDataService.getSortedExposedForKeyDate(
            receivedAt.minus(Duration.ofDays(2)), batchTime, batchTime.plusHours(2), now, true);
    assertEquals(0, returnedKeys.size());
  }

  private void insertExposeeWithReceivedAtAndKeyDate(
      Instant receivedAt, Instant keyDate, String key) throws SQLException {
    Connection connection = dataSource.getConnection();
    String sql =
        "into t_gaen_exposed (pk_exposed_id, key, received_at, rolling_start_number,"
            + " rolling_period, origin, share_with_federation_gateway) values (100, ?, ?, ?, 144,"
            + " 'CH', 'true')";
    PreparedStatement preparedStatement = connection.prepareStatement("insert " + sql);
    preparedStatement.setString(1, key);
    preparedStatement.setTimestamp(2, new Timestamp(receivedAt.toEpochMilli()));
    preparedStatement.setInt(
        3, (int) GaenUnit.TenMinutes.between(Instant.ofEpochMilli(0), keyDate));
    preparedStatement.execute();
  }

  protected void executeSQL(String sql) throws SQLException {
    try (final Connection connection = dataSource.getConnection();
        final PreparedStatement preparedStatement = connection.prepareStatement(sql); ) {
      preparedStatement.execute();
    }
  }
}
