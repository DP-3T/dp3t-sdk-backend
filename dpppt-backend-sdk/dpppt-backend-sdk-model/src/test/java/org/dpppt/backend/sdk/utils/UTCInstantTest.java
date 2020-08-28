package org.dpppt.backend.sdk.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class UTCInstantTest {

  @Test
  /*
   * Make sure that the duration is kept, and that it refuses to sleep if the duration already passed.
   */
  void normalizeDuration() throws Exception {
    UTCInstant now = UTCInstant.now();
    Duration minimumDuration = Duration.ofSeconds(1);
    UTCInstant oneSecondLater = now.plus(minimumDuration);
    assertTrue(UTCInstant.now().isBeforeEpochMillisOf(oneSecondLater));

    now.normalizeDuration(minimumDuration);
    assertFalse(UTCInstant.now().isBeforeEpochMillisOf(oneSecondLater));

    assertThrows(DurationExpiredException.class, () -> now.normalizeDuration(minimumDuration));
  }
}
