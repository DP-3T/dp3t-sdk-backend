package org.dpppt.backend.sdk.ws.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

public class GaenUnit implements TemporalUnit {
    public static GaenUnit TenMinutes = new GaenUnit();

    @Override
    public Duration getDuration() {
       
        return Duration.ofMinutes(10);
    }

    @Override
    public boolean isDurationEstimated() {
        return false;
    }

    @Override
    public boolean isDateBased() {
        return false;
    }

    @Override
    public boolean isTimeBased() {
        return true;
    }

    @Override
    public <R extends Temporal> R addTo(R temporal, long amount) {
        return (R)temporal.plus(this.getDuration().multipliedBy(amount));
    }

    @Override
    public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        var between = ChronoUnit.MINUTES.between(temporal1Inclusive,temporal2Exclusive) / 10;
        return between;
    }
    
}