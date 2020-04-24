package org.dpppt.backend.sdk.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.dpppt.backend.sdk.data.config.DPPPTDataServiceConfig;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.StandaloneDataConfig;
import org.dpppt.backend.sdk.model.Exposee;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { StandaloneDataConfig.class,
		FlyWayConfig.class, DPPPTDataServiceConfig.class })
@ActiveProfiles("hsqldb")
public class DPPPTDataServiceTest {

	@Autowired
	private DPPPTDataService dppptDataService;

	@Test
	public void testUpsertupsertExposee() {
		Exposee expected = new Exposee();
		expected.setKey("key");
		DateTime now = DateTime.now();
		expected.setKeyDate(now.withTimeAtStartOfDay().getMillis());

		dppptDataService.upsertExposee(expected, "AppSource");

		List<Exposee> sortedExposedForDay = dppptDataService.getSortedExposedForDay(now);
		assertFalse(sortedExposedForDay.isEmpty());
		Exposee actual = sortedExposedForDay.get(0);
		assertEquals(expected.getKey(), actual.getKey());
		assertEquals(expected.getKeyDate(), actual.getKeyDate());
		assertNotNull(actual.getId());
	}

	@Test
	public void testRedeemUUID() {
		boolean actual = dppptDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
		assertTrue(actual);
		actual = dppptDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
		assertFalse(actual);
		actual = dppptDataService.checkAndInsertPublishUUID("1c444adb-0924-4dc4-a7eb-1f52aa6b9575");
		assertTrue(actual);
	}

	@Test
	public void cleanUp() {
		Exposee expected = new Exposee();
		expected.setKey("key");
		DateTime now = DateTime.now();
		expected.setKeyDate(now.withTimeAtStartOfDay().getMillis());

		dppptDataService.upsertExposee(expected, "AppSource");
		dppptDataService.cleanDB(21);

		List<Exposee> sortedExposedForDay = dppptDataService.getSortedExposedForDay(now);
		assertFalse(sortedExposedForDay.isEmpty());

		dppptDataService.cleanDB(0);
		sortedExposedForDay = dppptDataService.getSortedExposedForDay(now);
		assertTrue(sortedExposedForDay.isEmpty());

	}
}