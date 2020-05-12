package org.dpppt.backend.sdk.ws.util;

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Test;

public class GaenUnitTest {
    @Test
    public void testTenMinutesAre10Minutes() throws Exception {
        var tenMinutes = Duration.of(1,GaenUnit.TenMinutes);
        var t10Minutes = Duration.ofMinutes(10);
        assertEquals(tenMinutes, t10Minutes);
    }
} 