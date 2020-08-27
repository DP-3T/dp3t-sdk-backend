package org.dpppt.backend.sdk.data.gaen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.dpppt.backend.sdk.data.config.DPPPTDataServiceConfig;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.RedeemDataServiceConfig;
import org.dpppt.backend.sdk.data.config.StandaloneDataConfig;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    loader = AnnotationConfigContextLoader.class,
    classes = {
      StandaloneDataConfig.class,
      FlyWayConfig.class,
      DPPPTDataServiceConfig.class,
      RedeemDataServiceConfig.class
    })
@ActiveProfiles("hsqldb")
public class GaenDataServiceTest {

  private static final Duration BUCKET_LENGTH = Duration.ofHours(2);

  @Autowired private GAENDataService gaenDataService;

  @Test
  public void upsert() throws Exception {
    var tmpKey = new GaenKey();
    tmpKey.setRollingStartNumber(
        (int) UTCInstant.today().minus(Duration.ofDays(1)).get10MinutesSince1970());
    tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
    tmpKey.setRollingPeriod(144);
    tmpKey.setFake(0);
    tmpKey.setTransmissionRiskLevel(0);
    var tmpKey2 = new GaenKey();
    tmpKey2.setRollingStartNumber(
        (int) UTCInstant.today().minus(Duration.ofDays(1)).get10MinutesSince1970());
    tmpKey2.setKeyData(Base64.getEncoder().encodeToString("testKey33Bytes--".getBytes("UTF-8")));
    tmpKey2.setRollingPeriod(144);
    tmpKey2.setFake(0);
    tmpKey2.setTransmissionRiskLevel(0);
    List<GaenKey> keys = List.of(tmpKey, tmpKey2);
    var utcNow = UTCInstant.now();
    gaenDataService.upsertExposees(keys, utcNow);

    long now = utcNow.getTimestamp();
    // calculate exposed until bucket, but get bucket in the future, as keys have
    // been inserted with timestamp now.
    long publishedUntil = now - (now % BUCKET_LENGTH.toMillis()) + BUCKET_LENGTH.toMillis();

    var returnedKeys =
        gaenDataService.getSortedExposedForKeyDate(
            UTCInstant.today().minusDays(1).getTimestamp(), null, publishedUntil);

    assertEquals(keys.size(), returnedKeys.size());
    assertEquals(keys.get(1).getKeyData(), returnedKeys.get(0).getKeyData());
  }
}
