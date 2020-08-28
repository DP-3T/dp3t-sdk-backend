package org.dpppt.backend.sdk.ws.insertmanager;

import java.time.Duration;
import java.util.List;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.utils.UTCInstant;

public class MockDataSource implements GAENDataService {

  @Override
  public void upsertExposees(List<GaenKey> keys, UTCInstant now) {
    throw new RuntimeException("UPSERT_EXPOSEES");
  }

  @Override
  public void upsertExposeesDelayed(
      List<GaenKey> keys, UTCInstant delayedReceivedAt, UTCInstant now) {}

  @Override
  public int getMaxExposedIdForKeyDate(Long keyDate, Long publishedAfter, Long publishedUntil) {
    return 0;
  }

  @Override
  public List<GaenKey> getSortedExposedForKeyDate(
      Long keyDate, Long publishedAfter, Long publishedUntil) {
    return null;
  }

  @Override
  public void cleanDB(Duration retentionPeriod) {}
}
