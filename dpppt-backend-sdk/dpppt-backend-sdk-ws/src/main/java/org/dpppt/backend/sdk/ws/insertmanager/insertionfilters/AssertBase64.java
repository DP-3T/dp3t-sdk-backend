package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.InsertException;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;

/**
 * Rejects a batch of keys if any of them have an invalid base64 encoding. Invalid base64 encodings
 * point to a client error.
 */
public class AssertBase64 implements KeyInsertionFilter {

  private final ValidationUtils validationUtils;

  public AssertBase64(ValidationUtils validationUtils) {
    this.validationUtils = validationUtils;
  }

  @Override
  public List<GaenKey> filter(
      UTCInstant now,
      List<GaenKey> content,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal)
      throws InsertException {

    var hasInvalidKeys =
        content.stream().anyMatch(key -> !validationUtils.isValidBase64Key(key.getKeyData()));

    if (hasInvalidKeys) {
      throw new KeyIsNotBase64Exception();
    }
    return content;
  }

  public class KeyIsNotBase64Exception extends InsertException {

    /** */
    private static final long serialVersionUID = -918099046973553472L;
  }
}
