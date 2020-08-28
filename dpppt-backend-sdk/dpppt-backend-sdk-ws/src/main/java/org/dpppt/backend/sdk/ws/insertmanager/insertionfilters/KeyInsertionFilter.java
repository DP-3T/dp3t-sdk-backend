package org.dpppt.backend.sdk.ws.insertmanager.insertionfilters;

import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.semver.Version;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.InsertException;
import org.dpppt.backend.sdk.ws.insertmanager.InsertManager;
import org.dpppt.backend.sdk.ws.insertmanager.OSType;

/** Interface for filters than can be configured in the {@link InsertManager} */
public interface KeyInsertionFilter {

  /**
   * The {@link InsertManager} goes through all configured filters and calls them with a list of
   * {@link GaenKey} where the filters are applied before inserting into the database.
   *
   * @param now current timestamp
   * @param content the list of new gaen keys for insertion
   * @param osType the os type of the client which uploaded the keys
   * @param osVersion the os version of the client which uploaded the keys
   * @param appVersion the app version of the client which uploaded the keys
   * @param principal the authorization context which belongs to the uploaded keys. Depending on the
   *     configured system, this could be a JWT token for example.
   * @return
   * @throws InsertException
   */
  public List<GaenKey> filter(
      UTCInstant now,
      List<GaenKey> content,
      OSType osType,
      Version osVersion,
      Version appVersion,
      Object principal)
      throws InsertException;
}
