package org.dpppt.backend.sdk.interops.insertmanager.insertionfilters;

import java.util.List;
import org.dpppt.backend.sdk.interops.insertmanager.InteropsInsertManager;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;

/** Interface for filters than can be configured in the {@link InteropsInsertManager} */
public interface InteropsKeyInsertionFilter {

  /**
   * The {@link InteropsInsertManager} goes through all configured filters and calls them with a
   * list of {@link GaenKeyForInterops} where the filters are applied before inserting into the
   * database.
   *
   * @param now current timestamp
   * @param content the list of new gaen keys for insertion
   * @return
   */
  public List<GaenKeyForInterops> filter(UTCInstant now, List<GaenKeyForInterops> content);

  public String getName();
}
