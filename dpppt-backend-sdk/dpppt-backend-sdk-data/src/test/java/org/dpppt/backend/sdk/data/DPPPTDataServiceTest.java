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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.dpppt.backend.sdk.data.config.DPPPTDataServiceConfig;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.RedeemDataServiceConfig;
import org.dpppt.backend.sdk.data.config.StandaloneDataConfig;
import org.dpppt.backend.sdk.model.Exposee;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { StandaloneDataConfig.class,
		FlyWayConfig.class, DPPPTDataServiceConfig.class, RedeemDataServiceConfig.class })
@ActiveProfiles("hsqldb")
public class DPPPTDataServiceTest {

	@Autowired
	private DPPPTDataService dppptDataService;
	@Autowired
	private RedeemDataService redeemDataService;

	@Test
	public void testUpsertupsertExposee() {
		Exposee expected = new Exposee();
		expected.setKey("key");
		OffsetDateTime now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
		expected.setKeyDate(now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());

		dppptDataService.upsertExposee(expected, "AppSource");

		List<Exposee> sortedExposedForDay = dppptDataService
				.getSortedExposedForBatchReleaseTime(OffsetDateTime.now().plusMinutes(10).toInstant().toEpochMilli(), 1 * 60 * 60 * 1000l);
		assertFalse(sortedExposedForDay.isEmpty());
		Exposee actual = sortedExposedForDay.get(0);
		assertEquals(expected.getKey(), actual.getKey());
		assertEquals(expected.getKeyDate(), actual.getKeyDate());
		assertNotNull(actual.getId());
	}

	@Test
	// depends on sorting of dbservice (in our case descsending with respect to id
	// -> last inserted is first in list)
	public void testUpsertExposees() {
		var expected = new ArrayList<Exposee>();
		var exposee1 = new Exposee();
		var exposee2 = new Exposee();
		exposee1.setKey("key1");
		exposee2.setKey("key2");

		OffsetDateTime now = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
		OffsetDateTime yesterday = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC).minusDays(1);
		exposee1.setKeyDate(now.toInstant().toEpochMilli());
		exposee2.setKeyDate(yesterday.toInstant().toEpochMilli());

		expected.add(exposee1);
		expected.add(exposee2);

		dppptDataService.upsertExposees(expected, "AppSource");

		List<Exposee> sortedExposedForDay = dppptDataService
				.getSortedExposedForBatchReleaseTime(OffsetDateTime.now().plusMinutes(10).toInstant().toEpochMilli(), 1 * 60 * 60 * 1000l);
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
	public void testRedeemUUID() {
		boolean actual = redeemDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
		assertTrue(actual);
		actual = redeemDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
		assertFalse(actual);
		actual = redeemDataService.checkAndInsertPublishUUID("1c444adb-0924-4dc4-a7eb-1f52aa6b9575");
		assertTrue(actual);
	}

	@Test
	public void cleanUp() {
		Exposee expected = new Exposee();
		expected.setKey("key");
		OffsetDateTime now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
		expected.setKeyDate(now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());

		dppptDataService.upsertExposee(expected, "AppSource");
		dppptDataService.cleanDB(Duration.ofDays(21));

		List<Exposee> sortedExposedForDay = dppptDataService
				.getSortedExposedForBatchReleaseTime(now.plusMinutes(10).toInstant().toEpochMilli(), 1 * 60 * 60 * 1000l);
		assertFalse(sortedExposedForDay.isEmpty());

		dppptDataService.cleanDB(Duration.ofDays(0));
		sortedExposedForDay = dppptDataService.getSortedExposedForBatchReleaseTime(now.plusMinutes(10).toInstant().toEpochMilli(),
				1 * 60 * 60 * 1000l);
		assertTrue(sortedExposedForDay.isEmpty());

	}
}