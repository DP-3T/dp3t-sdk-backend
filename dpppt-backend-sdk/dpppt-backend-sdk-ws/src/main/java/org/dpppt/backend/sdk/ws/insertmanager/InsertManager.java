package org.dpppt.backend.sdk.ws.insertmanager;

import java.util.ArrayList;
import java.util.List;
import org.dpppt.backend.sdk.data.gaen.DebugGAENDataService;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.insertionfilters.KeyInsertionFilter;
import org.dpppt.backend.sdk.ws.insertmanager.insertionmodifier.KeyInsertionModifier;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The insertion manager is responsible for inserting keys uploaded by clients into the database. To
 * make sure we only have valid keys in the database, a list of {@link KeyInsertionModifier} is
 * applied, and then a list of {@Link KeyInsertionFilter} is applied to the given list of keys. The
 * remaining keys are then inserted into the database. If any of the modifiers filters throws an
 * {@Link InsertException} the process of insertions is aborted and the exception is propagated back
 * to the caller, which is responsible for handling the exception.
 */
public class InsertManager {

  private final List<KeyInsertionFilter> filterList = new ArrayList<>();
  private final List<KeyInsertionModifier> modifierList = new ArrayList<>();

  private final GAENDataService dataService;
  private final ValidationUtils validationUtils;

  private DebugGAENDataService debugDataService;

  private static final Logger logger = LoggerFactory.getLogger(InsertManager.class);

  public InsertManager(GAENDataService dataService, ValidationUtils validationUtils) {
    this.dataService = dataService;
    this.validationUtils = validationUtils;
    this.debugDataService = null;
  }

  private InsertManager(DebugGAENDataService debugDataService, ValidationUtils validationUtils) {
    this.debugDataService = debugDataService;
    this.validationUtils = validationUtils;
    this.dataService = null;
  }

  public static InsertManager getDebugInsertManager(
      DebugGAENDataService debugDataService, ValidationUtils validationUtils) {
    return new InsertManager(debugDataService, validationUtils);
  }

  public void addFilter(KeyInsertionFilter filter) {
    this.filterList.add(filter);
  }

  public void addModifier(KeyInsertionModifier modifier) {
    this.modifierList.add(modifier);
  }

  /**
   * Inserts the keys into the database. The additional parameters are supplied to the configured
   * modifiers and filters.
   *
   * @param keys the list of keys from the client
   * @param header request header from client
   * @param principal key upload authorization, for example a JWT token.
   * @param now current timestamp to work with.
   * @throws InsertException filters are allowed to throw errors, for example to signal client
   *     errors in the key upload
   */
  public void insertIntoDatabase(
      List<GaenKey> keys, String header, Object principal, UTCInstant now, boolean international)
      throws InsertException {

    if (keys == null || keys.isEmpty()) {
      return;
    }
    var internalKeys = filterAndModify(keys, header, principal, now);
    // if no keys remain or this is a fake request, just return. Else, insert the
    // remaining keys.
    if (!internalKeys.isEmpty() && !validationUtils.jwtIsFake(principal)) {
      dataService.upsertExposees(internalKeys, now, international);
    }
  }

  public void insertIntoDatabaseDEBUG(
      String deviceName, List<GaenKey> keys, String header, Object principal, UTCInstant now)
      throws InsertException {
    if (keys == null || keys.isEmpty()) {
      return;
    }
    var internalKeys = filterAndModify(keys, header, principal, now);
    // if no keys remain or this is a fake request, just return. Else, insert the
    // remaining keys.
    if (!internalKeys.isEmpty() && !validationUtils.jwtIsFake(principal)) {
      debugDataService.upsertExposees(deviceName, internalKeys);
    }
  }

  private List<GaenKey> filterAndModify(
      List<GaenKey> keys, String header, Object principal, UTCInstant now) throws InsertException {
    if (debugDataService != null) {
      logger.warn("DebugDataService is not null, don't use this in production!");
    }
    var internalKeys = keys;
    var headerParts = header.split(";");
    if (headerParts.length < 5) {
      headerParts =
          List.of("org.example.dp3t", "1.0.0", "0", "Android", "29").toArray(new String[0]);
      logger.error("We received an invalid header, setting default.");
    }

    // Map the given headers to os type, os version and app version. Examples are:
    // ch.admin.bag.dp36;1.0.7;200724.1105.215;iOS;13.6
    // ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29
    var osType = exctractOS(headerParts[3]);
    var osVersion = extractOsVersion(headerParts[4]);
    var appVersion = extractAppVersion(headerParts[1], headerParts[2]);

    for (KeyInsertionModifier modifier : modifierList) {
      internalKeys = modifier.modify(now, internalKeys, osType, osVersion, appVersion, principal);
    }

    for (KeyInsertionFilter filter : filterList) {
      internalKeys = filter.filter(now, internalKeys, osType, osVersion, appVersion, principal);
    }
    return internalKeys;
  }

  /**
   * Extracts the {@link OSType} from the osString that is given by the client request.
   *
   * @param osString
   * @return
   */
  private OSType exctractOS(String osString) {
    var result = OSType.ANDROID;
    switch (osString.toLowerCase()) {
      case "ios":
        result = OSType.IOS;
        break;
      case "android":
        break;
      default:
        result = OSType.ANDROID;
    }
    return result;
  }

  private Version extractOsVersion(String osVersionString) {
    return new Version(osVersionString);
  }

  private Version extractAppVersion(String osAppVersionString, String osMetaInfo) {
    return new Version(osAppVersionString + "+" + osMetaInfo);
  }
}
