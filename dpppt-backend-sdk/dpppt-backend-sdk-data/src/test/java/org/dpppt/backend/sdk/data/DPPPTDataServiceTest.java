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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.dpppt.backend.sdk.data.config.DPPPTDataServiceConfig;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.RedeemDataServiceConfig;
import org.dpppt.backend.sdk.data.config.StandaloneDataConfig;
import org.dpppt.backend.sdk.data.config.TestConfig;
import org.dpppt.backend.sdk.model.Exposee;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    loader = AnnotationConfigContextLoader.class,
    classes = {
      StandaloneDataConfig.class,
      FlyWayConfig.class,
      DPPPTDataServiceConfig.class,
      RedeemDataServiceConfig.class,
      TestConfig.class
    })
@ActiveProfiles({"hsqldb", "test-config"})
@Transactional
public class DPPPTDataServiceTest {

  @Autowired private DPPPTDataService dppptDataService;
  @Autowired private RedeemDataService redeemDataService;

  @Test
  @Transactional
  public void testUpsertupsertExposee() {
    Exposee expected = new Exposee();
    expected.setKey("key");
    var now = UTCInstant.now();
    expected.setKeyDate(now.atStartOfDay().getTimestamp());

    dppptDataService.upsertExposee(expected, "AppSource");

    List<Exposee> sortedExposedForDay =
        dppptDataService.getSortedExposedForBatchReleaseTime(
            now.plusMinutes(10).getTimestamp(), 1 * 60 * 60 * 1000l);
    assertFalse(sortedExposedForDay.isEmpty());
    Exposee actual = sortedExposedForDay.get(0);
    assertEquals(expected.getKey(), actual.getKey());
    assertEquals(expected.getKeyDate(), actual.getKeyDate());
    assertNotNull(actual.getId());
  }

  @Test
  @Transactional
  // depends on sorting of dbservice (in our case descsending with respect to id
  // -> last inserted is first in list)
  public void testUpsertExposees() throws Exception {
    var expected = new ArrayList<Exposee>();
    var exposee1 = new Exposee();
    var exposee2 = new Exposee();
    exposee1.setKey("key1");
    exposee2.setKey("key2");

    var now = UTCInstant.now();
    var nowMidnight = now.atStartOfDay();
    var yesterday = nowMidnight.minusDays(1);
    exposee1.setKeyDate(nowMidnight.getTimestamp());
    exposee2.setKeyDate(yesterday.getTimestamp());

    expected.add(exposee1);
    expected.add(exposee2);

    dppptDataService.upsertExposees(expected, "AppSource");

    List<Exposee> sortedExposedForDay =
        dppptDataService.getSortedExposedForBatchReleaseTime(
            now.plusMinutes(10).getTimestamp(), 1 * 60 * 60 * 1000l);
    assertFalse(sortedExposedForDay.isEmpty());

    Exposee actual = sortedExposedForDay.get(1);
    assertEquals(expected.get(0).getKey(), actual.getKey());
    assertEquals(expected.get(0).getKeyDate(), actual.getKeyDate());
    assertNotNull(actual.getId());

    Exposee actualYesterday = sortedExposedForDay.get(0);
    assertEquals(expected.get(1).getKey(), actualYesterday.getKey());
    assertEquals(expected.get(1).getKeyDate(), actualYesterday.getKeyDate());
    assertNotNull(actualYesterday.getId());
  }

  @Test
  @Transactional
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
  @Transactional
  public void cleanUp() {
    Exposee expected = new Exposee();
    expected.setKey("key");
    var now = UTCInstant.now();
    expected.setKeyDate(now.atStartOfDay().getTimestamp());

    dppptDataService.upsertExposee(expected, "AppSource");
    dppptDataService.cleanDB(Duration.ofDays(21));

    List<Exposee> sortedExposedForDay =
        dppptDataService.getSortedExposedForBatchReleaseTime(
            now.plusMinutes(10).getTimestamp(), 1 * 60 * 60 * 1000l);
    assertFalse(sortedExposedForDay.isEmpty());

    dppptDataService.cleanDB(Duration.ofDays(0));
    sortedExposedForDay =
        dppptDataService.getSortedExposedForBatchReleaseTime(
            now.plusMinutes(10).getTimestamp(), 1 * 60 * 60 * 1000l);
    assertTrue(sortedExposedForDay.isEmpty());
  }
}
