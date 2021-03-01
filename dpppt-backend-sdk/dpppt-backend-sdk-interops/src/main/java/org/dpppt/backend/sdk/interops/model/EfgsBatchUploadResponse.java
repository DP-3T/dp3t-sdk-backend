package org.dpppt.backend.sdk.interops.model;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Multi-Status response from Upload. The payload returns three properties (201, 409 and 500) each
 * property contains a list of indexes. The index refers to the key position on the ordered
 * Diagnosis Keys from UploadPayload. 201 - Successfully added DO NOTHING 409 - Conflict: Key was
 * already added DO NOTHING 500 - Server Error: Key not processed RETRY
 */
public class EfgsBatchUploadResponse {

  @JsonProperty("409")
  private List<Integer> status409 = emptyList();

  @JsonProperty("500")
  private List<Integer> status500 = emptyList();

  @JsonProperty("201")
  private List<Integer> status201 = emptyList();

  /**
   * Create the BatchUploadResponse.
   *
   * @param status409 Conflict: Key was already added
   * @param status500 Server Error: Key not processed
   * @param status201 Successfully added
   */
  public EfgsBatchUploadResponse(
      List<Integer> status409, List<Integer> status500, List<Integer> status201) {
    this.status409 = status409;
    this.status500 = status500;
    this.status201 = status201;
  }

  @Override
  public String toString() {
    return "BatchUploadResponse{"
        + "status409="
        + status409
        + ", status500="
        + status500
        + ", status201="
        + status201
        + '}';
  }

  /** Create an empty BatchUploadResponse. */
  public EfgsBatchUploadResponse() {}

  public List<Integer> getStatus409() {
    return status409;
  }

  public List<Integer> getStatus500() {
    return status500;
  }

  public List<Integer> getStatus201() {
    return status201;
  }
}
