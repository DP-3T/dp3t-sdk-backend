package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

/**
 * This filter checks for valid rolling period. The rolling period must always be in [1..144],
 * otherwise the key is not valid and is filtered out. See <a href=
 * "https://github.com/google/exposure-notifications-server/blob/main/docs/server_functional_requirements.md#publishing-temporary-exposure-keys"
 * >EN documentation</a>
 */
public class ValidRollingPeriodFilter implements KeyInsertionFilter {

  /** Loop through given keys and filter out keys which have rolling period < 1 or > 144. */
  @Override
  public List<GaenKey> filter(
      UTCInstant now,
      List<GaenKey> content,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal) {
    return content.stream()
        .filter(key -> key.getRollingPeriod() >= 1 && key.getRollingPeriod() <= 144)
        .collect(Collectors.toList());
  }
}
