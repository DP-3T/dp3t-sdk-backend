package org.dpppt.backend.sdk.utils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalUnit;
import java.util.Date;

import org.dpppt.backend.sdk.model.gaen.GaenUnit;

public class UTCInstant {
    private final long timestamp;
    private static Clock currentClock = Clock.systemUTC();

    public static UTCInstant today() {
        return new UTCInstant(LocalDateTime.now(currentClock).toInstant(ZoneOffset.UTC)).atStartOfDay();
    }
    public static UTCInstant midnight1970(){
        return new UTCInstant(0);
    }

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
    public static void resetClock() {
        currentClock = Clock.systemUTC();
    }
    public static UTCInstant now() {
        var nowTimestamp = currentClock.millis();
        return new UTCInstant(nowTimestamp);
    }
    public static UTCInstant of(long amount, TemporalUnit unit) {
        return new UTCInstant(amount, unit);
    }
    public static UTCInstant ofEpochMillis(long epochMillis){
        return new UTCInstant(epochMillis);
    }
    public static UTCInstant parseDate(String dateString) {
        var timestamp = LocalDate.parse(dateString).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        return new UTCInstant(timestamp);
    }
    public static UTCInstant parseDateTime(String dateString) {
        var timestamp = LocalDateTime.parse(dateString).toInstant(ZoneOffset.UTC).toEpochMilli();
        return new UTCInstant(timestamp);
    }

    public Date getDate() {
        return Date.from(getInstant());
    }

    public Instant getInstant() {
        return Instant.ofEpochMilli(this.timestamp);
    }
    public Duration getDuration(long since) {
        return Duration.ofMillis(since);
    }
    public Duration getDuration(UTCInstant since) {
        return Duration.ofMillis(this.timestamp-since.timestamp);
    }
    public OffsetDateTime getOffsetDateTime() {
        return OffsetDateTime.ofInstant(getInstant(), ZoneOffset.UTC);
    }
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.ofInstant(getInstant(), ZoneOffset.UTC);
    }
    public UTCInstant atStartOfDay() {
        return new UTCInstant(getLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
    }
    public LocalDate getLocalDate() {
        return getLocalDateTime().toLocalDate();
    }
    public LocalTime getLocalTime() {
        return getLocalDateTime().toLocalTime();
    }

    public UTCInstant plus(Duration duration){
        return new UTCInstant(this.timestamp + duration.toMillis());
    } 
    public UTCInstant minus(Duration duration){
        return new UTCInstant(this.timestamp - duration.toMillis());
    } 
    //TODO: Fix for leap year and stuff
    public UTCInstant plusYears(long years){
        return new UTCInstant(this.timestamp + Duration.ofDays(years * 365).toMillis());
    } 
    //TODO: Fix for leap year and stuff
    public UTCInstant minusYears(long years){
        return new UTCInstant(this.timestamp - Duration.ofDays(years * 365).toMillis());
    } 
    public UTCInstant plusDays(long days){
        return new UTCInstant(this.timestamp + Duration.ofDays(days).toMillis());
    } 
    public UTCInstant minusDays(long days){
        return new UTCInstant(this.timestamp - Duration.ofDays(days).toMillis());
    } 
    public UTCInstant plusHours(long hours){
        return new UTCInstant(this.timestamp + Duration.ofHours(hours).toMillis());
    } 
    public UTCInstant minusHours(long hours){
        return new UTCInstant(this.timestamp - Duration.ofHours(hours).toMillis());
    } 
    public UTCInstant plusMinutes(long minutes){
        return new UTCInstant(this.timestamp + Duration.ofMinutes(minutes).toMillis());
    } 
    public UTCInstant minusMinutes(long minutes){
        return new UTCInstant(this.timestamp - Duration.ofMinutes(minutes).toMillis());
    } 
    public UTCInstant plusSeconds(long seconds){
        return new UTCInstant(this.timestamp + Duration.ofSeconds(seconds).toMillis());
    } 
    public UTCInstant minusSeconds(long seconds){
        return new UTCInstant(this.timestamp - Duration.ofSeconds(seconds).toMillis());
    } 

    public boolean isMidnight() {
        return getLocalTime().equals(LocalTime.MIDNIGHT);
    }
    public long getTimestamp() {
        return timestamp;
    }
    public long get10MinutesSince1970() {
        return getDuration(midnight1970()).dividedBy(GaenUnit.TenMinutes.getDuration());
    }



    public boolean isSameDate(UTCInstant other) {
        return this.getLocalDate().isEqual(other.getLocalDate());
    }
    public boolean isSameTime(UTCInstant other) {
        return this.getLocalTime().equals(other.getLocalTime());
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
    public boolean isBeforeDate(LocalDate other) {
        return this.getLocalDate().isBefore(other);
    }
    public boolean isAfterDate(LocalDate other) {
        return this.getLocalDate().isAfter(other);
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