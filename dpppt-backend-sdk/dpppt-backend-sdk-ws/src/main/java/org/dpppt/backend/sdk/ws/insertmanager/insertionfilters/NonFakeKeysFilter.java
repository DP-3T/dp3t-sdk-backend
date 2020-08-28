package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

/**
 * Filters out fake keys from fake upload requests. Only Non-Fake keys are inserted into the
 * database.
 */
public class NonFakeKeysFilter implements KeyInsertionFilter {

  /**
   * Loops through the list of given keys and checks the fake flag. Only return keys that have fake
   * flag set to 0
   */
  @Override
  public List<GaenKey> filter(
      UTCInstant now,
      List<GaenKey> content,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal) {
    return content.stream().filter(key -> key.getFake().equals(0)).collect(Collectors.toList());
  }
}
