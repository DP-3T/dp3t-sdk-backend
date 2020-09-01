package org.dpppt.backend.sdk.data.gaen;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeKeyService {

  private final GAENDataService dataService;
  private final Integer minNumOfKeys;
  private final SecureRandom random;
  private final Integer keySize;
  private final Duration retentionPeriod;
  private final boolean isEnabled;

  private static final Logger logger = LoggerFactory.getLogger(FakeKeyService.class);

  public FakeKeyService(
      GAENDataService dataService,
      Integer minNumOfKeys,
      Integer keySize,
      Duration retentionPeriod,
      boolean isEnabled)
      throws NoSuchAlgorithmException {
    this.dataService = dataService;
    this.minNumOfKeys = minNumOfKeys;
    this.random = new SecureRandom();
    this.keySize = keySize;
    this.retentionPeriod = retentionPeriod;
    this.isEnabled = isEnabled;
    this.updateFakeKeys();
  }

  public void updateFakeKeys() {
    deleteAllKeys();
    var currentKeyDate = UTCInstant.now();
    var tmpDate = currentKeyDate.minusDays(retentionPeriod.toDays());
    logger.debug("Fill Fake keys. Start: " + currentKeyDate + " End: " + tmpDate);
    do {
      var keys = new ArrayList<GaenKey>();
      for (int i = 0; i < minNumOfKeys; i++) {
        byte[] keyData = new byte[keySize];
        random.nextBytes(keyData);
        var keyGAENTime = (int) tmpDate.get10MinutesSince1970();
        var key = new GaenKey(Base64.getEncoder().encodeToString(keyData), keyGAENTime, 144, 0);
        keys.add(key);
      }
      // TODO: Check if currentKeyDate is indeed intended here
      this.dataService.upsertExposees(keys, currentKeyDate);
      tmpDate = tmpDate.plusDays(1);
    } while (tmpDate.isBeforeDateOf(currentKeyDate));
  }

  private void deleteAllKeys() {
    logger.debug("Delete all fake keys");
    this.dataService.cleanDB(Duration.ofDays(0));
  }

  public List<GaenKey> fillUpKeys(
      List<GaenKey> keys, UTCInstant publishedafter, UTCInstant keyDate) {
    if (!isEnabled) {
      return keys;
    }
    var today = UTCInstant.today();
    var keyLocalDate = keyDate.atStartOfDay();
    if (today.hasSameDateAs(keyLocalDate)) {
      return keys;
    }
    var fakeKeys =
        this.dataService.getSortedExposedForKeyDate(
            keyDate, publishedafter, UTCInstant.today().plusDays(1));

    keys.addAll(fakeKeys);
    return keys;
  }
}
