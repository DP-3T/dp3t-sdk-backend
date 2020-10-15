package org.dpppt.backend.sdk.data.gaen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.GaenDataServiceConfig;
import org.dpppt.backend.sdk.data.config.RedeemDataServiceConfig;
import org.dpppt.backend.sdk.data.config.StandaloneDataConfig;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    loader = AnnotationConfigContextLoader.class,
    classes = {
      StandaloneDataConfig.class,
      FlyWayConfig.class,
      GaenDataServiceConfig.class,
      RedeemDataServiceConfig.class
    })
@ActiveProfiles("hsqldb")
public class GaenDataServiceTest {

  private static final Duration BUCKET_LENGTH = Duration.ofHours(2);

  @Autowired private GAENDataService gaenDataService;

  @Test
  @Transactional
  public void upsert() throws Exception {
    var tmpKey = new GaenKey();
    tmpKey.setRollingStartNumber(
        (int) UTCInstant.today().minus(Duration.ofDays(1)).get10MinutesSince1970());
    tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes01".getBytes("UTF-8")));
    tmpKey.setRollingPeriod(144);
    tmpKey.setFake(0);
    tmpKey.setTransmissionRiskLevel(0);
    var tmpKey2 = new GaenKey();
    tmpKey2.setRollingStartNumber(
        (int) UTCInstant.today().minus(Duration.ofDays(1)).get10MinutesSince1970());
    tmpKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes02".getBytes("UTF-8")));
    tmpKey2.setRollingPeriod(144);
    tmpKey2.setFake(0);
    tmpKey2.setTransmissionRiskLevel(0);
    List<GaenKey> keys = List.of(tmpKey, tmpKey2);
    var now = UTCInstant.now();
    gaenDataService.upsertExposees(keys, now);

    // calculate exposed until bucket, but get bucket in the future, as keys have
    // been inserted with timestamp now.
    UTCInstant publishedUntil = now.roundToNextBucket(BUCKET_LENGTH);

    var returnedKeys =
        gaenDataService.getSortedExposedForKeyDate(
            UTCInstant.today().minusDays(1), null, publishedUntil, now);

    assertEquals(keys.size(), returnedKeys.size());
    assertEquals(keys.get(1).getKeyData(), returnedKeys.get(0).getKeyData());
  }

  @Test
  @Transactional
  public void testNoEarlyRelease() throws Exception {
    var outerNow = UTCInstant.now();
    Clock twoOClock =
        Clock.fixed(outerNow.atStartOfDay().plusHours(2).getInstant(), ZoneOffset.UTC);
    Clock elevenOClock =
        Clock.fixed(outerNow.atStartOfDay().plusHours(11).getInstant(), ZoneOffset.UTC);
    Clock fourteenOClock =
        Clock.fixed(outerNow.atStartOfDay().plusHours(14).getInstant(), ZoneOffset.UTC);

    try (var now = UTCInstant.setClock(twoOClock)) {
      var tmpKey = new GaenKey();
      tmpKey.setRollingStartNumber((int) now.atStartOfDay().get10MinutesSince1970());
      tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes--".getBytes("UTF-8")));
      tmpKey.setRollingPeriod(
          (int) Duration.ofHours(10).dividedBy(GaenUnit.TenMinutes.getDuration()));
      tmpKey.setFake(0);
      tmpKey.setTransmissionRiskLevel(0);

      gaenDataService.upsertExposees(List.of(tmpKey), now);
    }
    // key was inserted with a rolling period of 10 hours, which means the key is not allowed to be
    // released before 12, but since 12 already is in the 14 O'Clock bucket, it is not released
    // before 14:00

    // eleven O'clock no key
    try (var now = UTCInstant.setClock(elevenOClock)) {
      UTCInstant publishedUntil = now.roundToNextBucket(BUCKET_LENGTH).plusMinutes(1);
      var returnedKeys =
          gaenDataService.getSortedExposedForKeyDate(now.atStartOfDay(), null, publishedUntil, now);
      assertEquals(0, returnedKeys.size());
    }

    // twelve O'clock release the key
    try (var now = UTCInstant.setClock(fourteenOClock)) {
      UTCInstant publishedUntil = now.roundToNextBucket(BUCKET_LENGTH);
      var returnedKeys =
          gaenDataService.getSortedExposedForKeyDate(now.atStartOfDay(), null, publishedUntil, now);
      assertEquals(1, returnedKeys.size());
    }
  }

  @Test
  @Transactional
  public void upsertMultipleTimes() throws Exception {
    var tmpKey = new GaenKey();
    tmpKey.setRollingStartNumber(
        (int) UTCInstant.today().minus(Duration.ofDays(1)).get10MinutesSince1970());
    tmpKey.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes05".getBytes("UTF-8")));
    tmpKey.setRollingPeriod(144);
    tmpKey.setFake(0);
    tmpKey.setTransmissionRiskLevel(0);
    var tmpKey2 = new GaenKey();
    tmpKey2.setRollingStartNumber(
        (int) UTCInstant.today().minus(Duration.ofDays(1)).get10MinutesSince1970());
    tmpKey2.setKeyData(Base64.getEncoder().encodeToString("testKey32Bytes05".getBytes("UTF-8")));
    tmpKey2.setRollingPeriod(144);
    tmpKey2.setFake(0);
    tmpKey2.setTransmissionRiskLevel(0);
    List<GaenKey> keys = List.of(tmpKey, tmpKey2);
    var now = UTCInstant.now();
    gaenDataService.upsertExposees(keys, now);

    // calculate exposed until bucket, but get bucket in the future, as keys have
    // been inserted with timestamp now.
    UTCInstant publishedUntil = now.roundToNextBucket(BUCKET_LENGTH);

    var returnedKeys =
        gaenDataService.getSortedExposedForKeyDate(
            UTCInstant.today().minusDays(1), null, publishedUntil, now);

    assertEquals(1, returnedKeys.size());
    assertEquals(keys.get(1).getKeyData(), returnedKeys.get(0).getKeyData());
  }
}
