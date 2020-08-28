package org.dpppt.backend.sdk.ws.insertmanager.insertionmodifier;

import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.InsertException;
import org.dpppt.backend.sdk.ws.insertmanager.InsertManager;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

/** Interface for key modifiers than can be configured in the {@link InsertManager} */
public interface KeyInsertionModifier {

  /**
   * The {@link InsertManager} goes through all configured key modifiers and calls them with a list
   * of {@link GaenKey} where the modifieres are applied before inserting into the database.
   *
   * @param now current timestamp
   * @param content the list of new gaen keys for modification
   * @param osType the os type of the client which uploaded the keys
   * @param osVersion the os version of the client which uploaded the keys
   * @param appVersion the app version of the client which uploaded the keys
   * @param principal the authorization context which belongs to the uploaded keys. Depending on the
   *     configured system, this could be a JWT token for example.
   * @return
   * @throws InsertException
   */
  public List<GaenKey> modify(
      UTCInstant now,
      List<GaenKey> content,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal)
      throws InsertException;
}
