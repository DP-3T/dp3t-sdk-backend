package org.dpppt.backend.sdk.ws.util;

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.dpppt.backend.sdk.utils.UTCInstant;

import org.junit.Test;

public class ValidationUtilsTest {
    @Test
    public void testOnlyMidnightIsValid() throws Exception {
        var validationUtils = new ValidationUtils(16, Duration.ofDays(14), Duration.ofHours(2).toMillis());
        var midnight = UTCInstant.today();
        assertEquals(true, validationUtils.isValidKeyDate(midnight));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.minusSeconds(1)));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.minusMinutes(1)));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.minusHours(1)));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.plusSeconds(1)));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.plusMinutes(1)));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.plusHours(1)));
        
        assertEquals(true, UTCInstant.today().isSameDate(midnight));
    }
}