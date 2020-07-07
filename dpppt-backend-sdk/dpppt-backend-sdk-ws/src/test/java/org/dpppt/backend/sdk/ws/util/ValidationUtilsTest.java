package org.dpppt.backend.sdk.ws.util;

import static org.junit.Assert.assertEquals;

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
        assertEquals(true, validationUtils.isValidKeyDate(midnight.toInstant().toEpochMilli()));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.minusSeconds(1).toInstant().toEpochMilli()));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.minusMinutes(1).toInstant().toEpochMilli()));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.minusHours(1).toInstant().toEpochMilli()));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.plusSeconds(1).toInstant().toEpochMilli()));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.plusMinutes(1).toInstant().toEpochMilli()));
        assertEquals(false, validationUtils.isValidKeyDate(midnight.plusHours(1).toInstant().toEpochMilli()));
        
        assertEquals(true, LocalTime.MIDNIGHT.equals(midnight.toLocalTime()));
    }
}