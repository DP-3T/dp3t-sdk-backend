package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.ClaimIsBeforeOnsetException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.InvalidDateException;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.DelayedKeyDateClaimIsMissing;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.DelayedKeyDateIsInvalid;

/**
 * * This filter compares the supplied keys with information found in the JWT token. Depending on
 * the request, the following checks are made:
 *
 * <ul>
 *   <li>`exposed`: the key dates must be >= the onset date, which was set by the health authority
 *       and is available as a claim in the JWT
 *   <li>`exposednextday`: the supplied key must match `delayedKeyDate`, which has been set as a
 *       claim by a previous call to `exposed`
 * </ul>
 */
public class EnforceMatchingJWTClaims implements KeyInsertionFilter {

  private final ValidateRequest validateRequest;
  private final ValidationUtils validationUtils;

  public EnforceMatchingJWTClaims(ValidateRequest validateRequest, ValidationUtils utils) {
    this.validateRequest = validateRequest;
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
                // Found a delayedKeyDate claim, so it must be `/exposednextday`
                var delayedKeyDate =
                    UTCInstant.of(key.getRollingStartNumber(), GaenUnit.TenMinutes);
                return delayedKeyDateClaim.equals(delayedKeyDate)
                    && isValidDelayedKeyDate(now, delayedKeyDate);

              } catch (DelayedKeyDateClaimIsMissing ex) {
                // Didn't find a delayedKeyDate claim, so it must be `/exposed`
                return isValidKeyDate(key, principal, now);
              }
            })
        .collect(Collectors.toList());
  }

  private boolean isValidKeyDate(GaenKey key, Object principal, UTCInstant now) {
    try {
      validateRequest.validateKeyDate(now, principal, key);
      return true;
    } catch (InvalidDateException | ClaimIsBeforeOnsetException es) {
      return false;
    }
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
