package org.dpppt.backend.sdk.interops.insertmanager.insertionfilters;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;

/** Filters out keys with invalid base64 encoding or incorrect length. */
public class AssertKeyFormat implements InteropsKeyInsertionFilter {

  private final int gaenKeySizeBytes;

  public AssertKeyFormat(int gaenKeySizeBytes) {
    this.gaenKeySizeBytes = gaenKeySizeBytes;
  }

  @Override
  public List<GaenKeyForInterops> filter(UTCInstant now, List<GaenKeyForInterops> content) {
    return content.stream()
        .filter(key -> isValidKeyFormat(key.getKeyData()))
        .collect(Collectors.toList());
  }

  private boolean isValidKeyFormat(String value) {
    try {
      byte[] key = Base64.getDecoder().decode(value);
      return key.length == gaenKeySizeBytes;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String getName() {
    return "AssertKeyFormat";
  }
}
