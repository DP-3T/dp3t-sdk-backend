package org.dpppt.backend.sdk.ws.insertmanager.insertionmodifier;

import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some early builds of Google's Exposure Notification API returned TEKs with rolling period set to
 * '0'. According to the specification, this is invalid and will cause both Android and iOS to
 * drop/ignore the key. To mitigate ignoring TEKs from these builds altogether, the rolling period
 * is increased to '144' (one full day). This should not happen anymore and can be removed in the
 * near future. Until then we are going to log whenever this happens to be able to monitor this
 * problem.
 */
public class OldAndroid0RPModifier implements KeyInsertionModifier {

  private static final Logger logger = LoggerFactory.getLogger(OldAndroid0RPModifier.class);

  /**
   * Loop through all the given keys and check if the rolling period is equal to 0. If so, set to
   * 144. In case a key with rolling period 0 is received from an iOS client, an error log is
   * printed.
   */
  @Override
  public List<GaenKey> modify(
      UTCInstant now,
      List<GaenKey> content,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal) {
    for (GaenKey gaenKey : content) {
      if (gaenKey.getRollingPeriod().equals(0)) {
        if (osType.equals(OSType.IOS)) {
          logger.error("We got a rollingPeriod of 0 ({},{},{})", osType, osVersion, appVersion);
        }
        gaenKey.setRollingPeriod(144);
      }
    }
    return content;
  }
}
