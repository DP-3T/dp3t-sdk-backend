package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;

/**
 * Checks if a key is in the configured retention period. If a key is before the retention period it
 * is filtered out, as it will not be relevant for the system anymore.
 */
public class EnforceRetentionPeriod implements KeyInsertionFilter {

  private final ValidationUtils validationUtils;

  public EnforceRetentionPeriod(ValidationUtils validationUtils) {
    this.validationUtils = validationUtils;
  }

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
              var timestamp = UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
              return !validationUtils.isBeforeRetention(timestamp, now);
            })
        .collect(Collectors.toList());
  }
}
