package org.dpppt.backend.sdk.utils;

/** Indicates that the requested maximum duration already expired. */
public class DurationExpiredException extends Exception {
  public DurationExpiredException(String errorMessage) {
    super(errorMessage);
  }
}
