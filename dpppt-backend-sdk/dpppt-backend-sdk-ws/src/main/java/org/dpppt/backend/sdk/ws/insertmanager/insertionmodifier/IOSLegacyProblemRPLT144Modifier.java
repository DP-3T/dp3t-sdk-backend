package org.dpppt.backend.sdk.ws.insertmanager.insertionmodifier;

import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

/**
 * Overwrite the rolling period with the default value of 144 so that iOS does not reject the keys.
 * Since version 1.5 of the GAEN on Android, TEKs with a rolling period < 144 can be released.
 * Unfortunately these keys are rejected by iOS, so this filter sets the default value. There are
 * two downsides to this:
 *
 * <ul>
 *   <li>some more work for the GAEN to verify the keys
 *   <li>a same-day key with original rolling period < 144 will be released later and thus delay
 *       detection of an eventual exposition
 * </ul>
 */
public class IOSLegacyProblemRPLT144Modifier implements KeyInsertionModifier {

  @Override
  public List<GaenKey> modify(
      UTCInstant now,
      List<GaenKey> content,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal) {
    for (GaenKey key : content) {
      key.setRollingPeriod(144);
    }
    return content;
  }
}
