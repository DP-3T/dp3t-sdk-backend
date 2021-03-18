package org.dpppt.backend.sdk.utils;

import static org.dpppt.backend.sdk.utils.EfgsDsosUtil.DSOS_SYMPTOMATIC_UNKNOWN_ONSET_ZERO_POINT;
import static org.junit.Assert.assertEquals;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.junit.jupiter.api.Test;

public class EfgsDsosUtilTest {
  @Test
  void testDsosMapping() {
    var now = UTCInstant.now();

    testWithSubmissionTime(now.atStartOfDay().plusHours(1));
    testWithSubmissionTime(now.atStartOfDay().plusHours(14));
    testWithSubmissionTime(now.atStartOfDay().plusHours(23));
  }

  void testWithSubmissionTime(UTCInstant submissionTime) {

    GaenKeyForInterops submissionDateKey = new GaenKeyForInterops();
    submissionDateKey.setGaenKey(new GaenKey());
    submissionDateKey.setReceivedAt(submissionTime);
    submissionDateKey.setRollingStartNumber(
        (int) submissionTime.atStartOfDay().get10MinutesSince1970());

    assertEquals(
        DSOS_SYMPTOMATIC_UNKNOWN_ONSET_ZERO_POINT,
        EfgsDsosUtil.calculateDefaultDsosMapping(submissionDateKey));

    GaenKeyForInterops submissionPlus3Key = new GaenKeyForInterops();
    submissionPlus3Key.setGaenKey(new GaenKey());
    submissionPlus3Key.setReceivedAt(submissionTime);
    submissionPlus3Key.setRollingStartNumber(
        (int) submissionTime.plusDays(3).atStartOfDay().get10MinutesSince1970());

    assertEquals(
        DSOS_SYMPTOMATIC_UNKNOWN_ONSET_ZERO_POINT + 3,
        EfgsDsosUtil.calculateDefaultDsosMapping(submissionPlus3Key));

    GaenKeyForInterops submissionMinus10Key = new GaenKeyForInterops();
    submissionMinus10Key.setGaenKey(new GaenKey());
    submissionMinus10Key.setReceivedAt(submissionTime);
    submissionMinus10Key.setRollingStartNumber(
        (int) submissionTime.minusDays(10).atStartOfDay().get10MinutesSince1970());

    assertEquals(
        DSOS_SYMPTOMATIC_UNKNOWN_ONSET_ZERO_POINT - 10,
        EfgsDsosUtil.calculateDefaultDsosMapping(submissionMinus10Key));
  }
}
