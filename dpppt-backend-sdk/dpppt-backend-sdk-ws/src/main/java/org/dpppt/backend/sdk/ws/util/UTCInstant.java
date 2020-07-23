package org.dpppt.backend.sdk.ws.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalUnit;

import org.dpppt.backend.sdk.model.gaen.GaenUnit;

public class UTCInstant {
    private final long timestamp;
    private static Clock currentClock = Clock.systemUTC();

    public UTCInstant(long timestamp) {
        this.timestamp = timestamp;
    }
    public UTCInstant(Duration duration) {
        this.timestamp = duration.toMillis();
    }
    public UTCInstant(Instant instant) {
        this.timestamp = instant.toEpochMilli();
    }
    public UTCInstant(long units, TemporalUnit interval) {
        this.timestamp = units * interval.getDuration().toMillis();
    }
    public UTCInstant(OffsetDateTime offsetDateTime) {
        this.timestamp = offsetDateTime.toInstant().toEpochMilli();
    }
    public static void setClock(Clock clock) {
        currentClock = clock;
    }
    public static UTCInstant now() {
        var nowTimestamp = currentClock.millis();
        return new UTCInstant(nowTimestamp);
    }

    public Instant getInstant() {
        return Instant.ofEpochMilli(this.timestamp);
    }
    public Duration getDuration() {
        return Duration.ofMillis(this.timestamp);
    }
    public OffsetDateTime getOffsetDateTime() {
        return OffsetDateTime.ofInstant(getInstant(), ZoneOffset.UTC);
    }
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.ofInstant(getInstant(), ZoneOffset.UTC);
    }
    public LocalDate getLocalDate() {
        return getLocalDateTime().toLocalDate();
    }
    public LocalTime getLocalTime() {
        return getLocalDateTime().toLocalTime();
    }
    public boolean isMidnight() {
        return getLocalTime().equals(LocalTime.MIDNIGHT);
    }
    public long getTimestamp() {
        return timestamp;
    }
    public long get10MinutesSince1970() {
        return getDuration().dividedBy(GaenUnit.TenMinutes.getDuration());
    }

    public boolean isBeforeExact(UTCInstant other) {
        return this.getTimestamp() < other.getTimestamp();
    }
    public boolean isAfterExact(UTCInstant other) {
        return this.getTimestamp() > other.getTimestamp();
    }
    public boolean isBeforeDate(UTCInstant other) {
        return this.getLocalDate().isBefore(other.getLocalDate());
    }
    public boolean isAfterDate(UTCInstant other) {
        return this.getLocalDate().isAfter(other.getLocalDate());
    }
    public boolean isBeforeTime(UTCInstant other) {
        return this.getLocalTime().isBefore(other.getLocalTime());
    }
    public boolean isAfterTime(UTCInstant other) {
        return this.getLocalTime().isAfter(other.getLocalTime());
    }
    public boolean isBeforeToday() {
        return this.isBeforeDate(UTCInstant.now());
    }
    public boolean isAfterToday() {
        return this.isAfterDate(UTCInstant.now());
    }
}