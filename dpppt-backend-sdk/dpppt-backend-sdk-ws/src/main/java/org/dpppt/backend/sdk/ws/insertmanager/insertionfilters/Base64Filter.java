package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.InsertException;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;

/**
 * All keys must be valid Base64 encoded. Non valid Base64 keys are not allowed and are filtered
 * out. This filter rejects the whole submitted batch of keys, if any of the keys is not valid
 * Base64, as this is a client error.
 */
public class Base64Filter implements KeyInsertionFilter {

  private final ValidationUtils validationUtils;

  public Base64Filter(ValidationUtils validationUtils) {
    this.validationUtils = validationUtils;
  }

  /**
   * Loop through all keys and check for Base64 validity using {@link
   * ValidationUtils#isValidBase64Key(String)} and count the number of invalid keys. If the count is
   * > 0, a {@link KeyIsNotBase64Exception} is thrown which results in a client error.
   */
  @Override
  public List<GaenKey> filter(
      UTCInstant now,
      List<GaenKey> content,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal)
      throws InsertException {

    var numberOfInvalidKeys =
        content.stream().filter(key -> !validationUtils.isValidBase64Key(key.getKeyData())).count();

    if (numberOfInvalidKeys > 0) {
      throw new KeyIsNotBase64Exception();
    }
    return content;
  }

  public class KeyIsNotBase64Exception extends InsertException {

    /** */
    private static final long serialVersionUID = -918099046973553472L;
  }
}
