package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.DelayedKeyDateClaimIsMissing;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.DelayedKeyDateIsInvalid;

/**
 * This filter compares the supplied keys from the exposed next day request with information found
 * in the JWT token: the supplied key must match `delayedKeyDate`, which has been set as a claim by
 * a previous call to `exposed`
 */
public class EnforceMatchingJWTClaimsForExposedNextDay implements KeyInsertionFilter {

  private final ValidationUtils validationUtils;

  public EnforceMatchingJWTClaimsForExposedNextDay(ValidationUtils utils) {
    this.validationUtils = utils;
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
              try {
                // getDelayedKeyDateClaim throws an exception if there is no delayedKeyDate claim
                // available.
                var delayedKeyDateClaim = validationUtils.getDelayedKeyDateClaim(principal);
                var delayedKeyDate =
                    UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
                return delayedKeyDateClaim.equals(delayedKeyDate)
                    && isValidDelayedKeyDate(now, delayedKeyDate);
              } catch (DelayedKeyDateClaimIsMissing ex) {
                return false;
              }
            })
        .collect(Collectors.toList());
  }

  private boolean isValidDelayedKeyDate(UTCInstant now, UTCInstant delayedKeyDate) {
    try {
      validationUtils.assertDelayedKeyDate(now, delayedKeyDate);
      return true;
    } catch (DelayedKeyDateIsInvalid ex) {
      return false;
    }
  }
}
