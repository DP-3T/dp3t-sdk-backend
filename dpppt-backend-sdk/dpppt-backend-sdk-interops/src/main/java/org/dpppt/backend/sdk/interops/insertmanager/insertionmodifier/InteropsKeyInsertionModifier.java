package org.dpppt.backend.sdk.interops.insertmanager.insertionmodifier;

import java.util.List;
import org.dpppt.backend.sdk.interops.insertmanager.InteropsInsertManager;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;

/** Interface for key modifiers than can be configured in the {@link InteropsInsertManager} */
public interface InteropsKeyInsertionModifier {

  /**
   * The {@link InteropsInsertManager} goes through all configured key modifiers and calls them with
   * a list of {@link GaenKeyForInterops} where the modifieres are applied before inserting into the
   * database.
   *
   * @param now current timestamp
   * @param content the list of new gaen keys for modification
   * @return
   */
  public List<GaenKeyForInterops> modify(UTCInstant now, List<GaenKeyForInterops> content);
}
