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

/**
 * This class collects all usages of time within the project. Any `java.time.*` class should have
 * its equivalent regarding UTC.
 *
 * <p>All timestamps should be described as 'milliseconds since Unix epoch'.
 *
 * <p>IMPORTANT: `Local*` classes do not carry any timezone informations. As such, all comparisons
 * are made regarding 'UTC'
 */
public class UTCInstant {
  private final long timestamp;
  private static Clock currentClock = Clock.systemUTC();

  public static UTCInstant today() {
    return UTCInstant.now().atStartOfDay();
  }

  public static UTCInstant midnight1970() {
    return new UTCInstant(0);
  }

  public UTCInstant(long timestamp) {
    this.timestamp = timestamp;
  }

  public UTCInstant(Duration duration, UTCInstant since) {
    this.timestamp = since.timestamp + duration.toMillis();
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

  // TODO: make protected and subclass for use in tests
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

  public static UTCInstant ofEpochMillis(Long epochMillis) {
    return new UTCInstant(epochMillis == null ? 0 : epochMillis);
  }

  public static UTCInstant parseDate(String dateString) {
    var timestamp =
        LocalDate.parse(dateString).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
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
    return Duration.ofMillis(this.timestamp - since);
  }

  public Duration getDuration(UTCInstant since) {
    return Duration.ofMillis(this.timestamp - since.timestamp);
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

  public UTCInstant roundToPreviousBucket(Duration releaseBucketDuration) {
    var roundedTimestamp =
        (long) Math.floor(this.timestamp / releaseBucketDuration.toMillis())
            * releaseBucketDuration.toMillis();
    return new UTCInstant(roundedTimestamp);
  }

  public UTCInstant roundToNextBucket(Duration releaseBucketDuration) {
    var roundedTimestamp =
        ((long) Math.floor(this.timestamp / releaseBucketDuration.toMillis()) + 1)
            * releaseBucketDuration.toMillis();
    return new UTCInstant(roundedTimestamp);
  }

  public UTCInstant plus(Duration duration) {
    return new UTCInstant(this.timestamp + duration.toMillis());
  }

  public UTCInstant minus(Duration duration) {
    return new UTCInstant(this.timestamp - duration.toMillis());
  }

  public UTCInstant plusYears(long years) {
    return new UTCInstant(this.getOffsetDateTime().plusYears(years));
  }

  public UTCInstant minusYears(long years) {
    return new UTCInstant(this.getOffsetDateTime().minusYears(years));
  }

  public UTCInstant plusDays(long days) {
    return new UTCInstant(this.timestamp + Duration.ofDays(days).toMillis());
  }

  public UTCInstant minusDays(long days) {
    return new UTCInstant(this.timestamp - Duration.ofDays(days).toMillis());
  }

  public UTCInstant plusHours(long hours) {
    return new UTCInstant(this.timestamp + Duration.ofHours(hours).toMillis());
  }

  public UTCInstant minusHours(long hours) {
    return new UTCInstant(this.timestamp - Duration.ofHours(hours).toMillis());
  }

  public UTCInstant plusMinutes(long minutes) {
    return new UTCInstant(this.timestamp + Duration.ofMinutes(minutes).toMillis());
  }

  public UTCInstant minusMinutes(long minutes) {
    return new UTCInstant(this.timestamp - Duration.ofMinutes(minutes).toMillis());
  }

  public UTCInstant plusSeconds(long seconds) {
    return new UTCInstant(this.timestamp + Duration.ofSeconds(seconds).toMillis());
  }

  public UTCInstant minusSeconds(long seconds) {
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

  public boolean hasSameDateAs(UTCInstant otherInstant) {
    return this.getLocalDate().isEqual(otherInstant.getLocalDate());
  }

  public boolean hasSameTimeOfDayAs(UTCInstant otherInstant) {
    return this.getLocalTime().equals(otherInstant.getLocalTime());
  }

  public boolean isBeforeEpochMillisOf(UTCInstant otherInstant) {
    return this.getTimestamp() < otherInstant.getTimestamp();
  }

  public boolean isAfterEpochMillisOf(UTCInstant otherInstant) {
    return this.getTimestamp() > otherInstant.getTimestamp();
  }

  public boolean isBeforeDateOf(UTCInstant otherInstant) {
    return this.getLocalDate().isBefore(otherInstant.getLocalDate());
  }

  public boolean isAfterDateOf(UTCInstant otherInstant) {
    return this.getLocalDate().isAfter(otherInstant.getLocalDate());
  }

  public boolean isBeforeDateOf(LocalDate otherDate) {
    return this.getLocalDate().isBefore(otherDate);
  }

  public boolean isAfterDateOf(LocalDate otherDate) {
    return this.getLocalDate().isAfter(otherDate);
  }

  public boolean isBeforeTimeOfDayOf(UTCInstant otherInstant) {
    return this.getLocalTime().isBefore(otherInstant.getLocalTime());
  }

  public boolean isAfterTimeOfDayOf(UTCInstant otherInstant) {
    return this.getLocalTime().isAfter(otherInstant.getLocalTime());
  }

  public boolean isBeforeToday() {
    return this.isBeforeDateOf(UTCInstant.now());
  }

  public boolean isAfterToday() {
    return this.isAfterDateOf(UTCInstant.now());
  }

  /**
   * To avoid timing attacks where the duration of the API is used to infer what the user requested,
   * all requests that change the database call this method to have the same duration, so an outside
   * attacker cannot infer anything on the response time. If the caller already spent too much time,
   * an exception is thrown.
   *
   * @param totalDuration how long the total duration should be
   * @throws InterruptedException if the sleep failed
   * @throws DurationExpiredException if the duration already passed
   */
  public void normalizeDuration(Duration totalDuration)
      throws InterruptedException, DurationExpiredException {
    Duration timeFillUp = totalDuration.minus(UTCInstant.now().getDuration(this));
    if (timeFillUp.isNegative()) {
      throw new DurationExpiredException("Duration of call was longer than requestDuration");
    } else {
      Thread.sleep(timeFillUp.toMillis());
    }
  }
}
