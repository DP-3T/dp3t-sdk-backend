package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

/**
 * Checks if a key has rolling start number after the day after tomorrow. If so, the key is filtered
 * out, as this is not allowed by the system to insert keys too far in the future.
 */
public class RollingStartNumberAfterDayAfterTomorrowFilter implements KeyInsertionFilter {

  /**
   * Loops through all the keys and converts the rolling start number to a timstamp. The it is
   * checked if the timestamp is before now + 2 days.
   */
  @Override
  public List<GaenKey> filter(
      UTCInstant now,
      List<GaenKey> content,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal) {
    return content.stream()
        .filter(
            key -> {
              var rollingStartNumberInstant =
                  UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
              return rollingStartNumberInstant.isBeforeDateOf(now.plusDays(2));
            })
        .collect(Collectors.toList());
  }
}
