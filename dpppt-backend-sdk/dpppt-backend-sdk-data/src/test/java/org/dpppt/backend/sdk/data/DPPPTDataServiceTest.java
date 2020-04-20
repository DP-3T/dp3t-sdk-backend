package org.dpppt.backend.sdk.data;

import org.dpppt.backend.sdk.data.config.DPPPTDataServiceConfig;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.StandaloneDataConfig;
import org.dpppt.backend.sdk.model.Exposee;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {StandaloneDataConfig.class,
        FlyWayConfig.class, DPPPTDataServiceConfig.class})
@ActiveProfiles("hsqldb")
public class DPPPTDataServiceTest {

    private DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");

    @Autowired
    private DPPPTDataService dppptDataService;

    @Test
    public void testUpsertupsertExposee() {
        Exposee expected = new Exposee();
        expected.setKey("key");
        DateTime now = DateTime.now();
        expected.setOnset(fmt.print(now));

        dppptDataService.upsertExposee(expected,"AppSource");

        List<Exposee> sortedExposedForDay = dppptDataService.getSortedExposedForDay(now);
        assertFalse(sortedExposedForDay.isEmpty());
        Exposee actual = sortedExposedForDay.get(0);
        assertEquals(expected.getKey(), actual.getKey());
        assertEquals(expected.getOnset(), actual.getOnset());
        assertNotNull(actual.getId());
    }
}