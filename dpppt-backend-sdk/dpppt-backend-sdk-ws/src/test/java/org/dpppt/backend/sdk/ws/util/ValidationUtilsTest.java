package org.dpppt.backend.sdk.ws.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.junit.Test;

public class ValidationUtilsTest {
    @Test
    public void testOnlyMidnightIsValid() throws Exception {
        var validationUtils = new ValidationUtils(16, Duration.ofDays(14), Duration.ofHours(2).toMillis());
        var midnight = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC);
        assertTrue(validationUtils.isValidKeyDate(midnight.toInstant().toEpochMilli()));
        assertFalse(validationUtils.isValidKeyDate(midnight.minusSeconds(1).toInstant().toEpochMilli()));
        assertFalse(validationUtils.isValidKeyDate(midnight.minusMinutes(1).toInstant().toEpochMilli()));
        assertFalse(validationUtils.isValidKeyDate(midnight.minusHours(1).toInstant().toEpochMilli()));
        assertFalse(validationUtils.isValidKeyDate(midnight.plusSeconds(1).toInstant().toEpochMilli()));
        assertFalse(validationUtils.isValidKeyDate(midnight.plusMinutes(1).toInstant().toEpochMilli()));
        assertFalse(validationUtils.isValidKeyDate(midnight.plusHours(1).toInstant().toEpochMilli()));
        
        assertTrue(LocalTime.MIDNIGHT.equals(midnight.toLocalTime()));
    }
}