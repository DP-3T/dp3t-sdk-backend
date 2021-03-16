package org.dpppt.backend.sdk.interops;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.DsosFilter;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class DsosFilterTest {
  @Test
  public void testDsos7DayRange() {
    List<GaenKeyForInterops> keys = new ArrayList<>();

    GaenKeyForInterops startRangeMinus3 = new GaenKeyForInterops();
    startRangeMinus3.setDaysSinceOnsetOfSymptoms(691);
    keys.add(startRangeMinus3);

    GaenKeyForInterops startRangeMinus2 = new GaenKeyForInterops();
    startRangeMinus2.setDaysSinceOnsetOfSymptoms(692);
    keys.add(startRangeMinus2);

    GaenKeyForInterops startRangeMinus1 = new GaenKeyForInterops();
    startRangeMinus1.setDaysSinceOnsetOfSymptoms(693);
    keys.add(startRangeMinus1);

    // Start range
    GaenKeyForInterops day1 = new GaenKeyForInterops();
    day1.setDaysSinceOnsetOfSymptoms(694);
    keys.add(day1);

    GaenKeyForInterops day2 = new GaenKeyForInterops();
    day2.setDaysSinceOnsetOfSymptoms(695);
    keys.add(day2);

    GaenKeyForInterops day3 = new GaenKeyForInterops();
    day3.setDaysSinceOnsetOfSymptoms(696);
    keys.add(day3);

    GaenKeyForInterops day4 = new GaenKeyForInterops();
    day4.setDaysSinceOnsetOfSymptoms(697);
    keys.add(day4);

    GaenKeyForInterops day5 = new GaenKeyForInterops();
    day5.setDaysSinceOnsetOfSymptoms(698);
    keys.add(day5);

    GaenKeyForInterops day6 = new GaenKeyForInterops();
    day6.setDaysSinceOnsetOfSymptoms(699);
    keys.add(day6);

    // End of range
    GaenKeyForInterops day7 = new GaenKeyForInterops();
    day7.setDaysSinceOnsetOfSymptoms(700);
    keys.add(day7);

    // Submission day
    GaenKeyForInterops day8 = new GaenKeyForInterops();
    day8.setDaysSinceOnsetOfSymptoms(701);
    keys.add(day8);

    DsosFilter filter = new DsosFilter();
    List<GaenKeyForInterops> filteredKeys = filter.filter(UTCInstant.now(), keys);
    assertEquals(10, filteredKeys.size());
  }

  @Test
  public void testDsosAsymptomatic() {
    List<GaenKeyForInterops> keys = new ArrayList<>();

    GaenKeyForInterops submissionMinus4 = new GaenKeyForInterops();
    submissionMinus4.setDaysSinceOnsetOfSymptoms(2996);
    keys.add(submissionMinus4);

    GaenKeyForInterops submissionMinus3 = new GaenKeyForInterops();
    submissionMinus3.setDaysSinceOnsetOfSymptoms(2997);
    keys.add(submissionMinus3);

    GaenKeyForInterops submissionMinus2 = new GaenKeyForInterops();
    submissionMinus2.setDaysSinceOnsetOfSymptoms(2998);
    keys.add(submissionMinus2);

    GaenKeyForInterops submissionMinus1 = new GaenKeyForInterops();
    submissionMinus1.setDaysSinceOnsetOfSymptoms(2999);
    keys.add(submissionMinus1);

    // Submission day
    GaenKeyForInterops submissionDay = new GaenKeyForInterops();
    submissionDay.setDaysSinceOnsetOfSymptoms(3000);
    keys.add(submissionDay);

    DsosFilter filter = new DsosFilter();
    List<GaenKeyForInterops> filteredKeys = filter.filter(UTCInstant.now(), keys);
    assertEquals(3, filteredKeys.size());
  }

  @Test
  public void testDsosSymptomaticOnsetUnknown() {
    List<GaenKeyForInterops> keys = new ArrayList<>();

    GaenKeyForInterops submissionMinus4 = new GaenKeyForInterops();
    submissionMinus4.setDaysSinceOnsetOfSymptoms(1996);
    keys.add(submissionMinus4);

    GaenKeyForInterops submissionMinus3 = new GaenKeyForInterops();
    submissionMinus3.setDaysSinceOnsetOfSymptoms(1997);
    keys.add(submissionMinus3);

    GaenKeyForInterops submissionMinus2 = new GaenKeyForInterops();
    submissionMinus2.setDaysSinceOnsetOfSymptoms(1998);
    keys.add(submissionMinus2);

    GaenKeyForInterops submissionMinus1 = new GaenKeyForInterops();
    submissionMinus1.setDaysSinceOnsetOfSymptoms(1999);
    keys.add(submissionMinus1);

    // Submission day
    GaenKeyForInterops submissionDay = new GaenKeyForInterops();
    submissionDay.setDaysSinceOnsetOfSymptoms(2000);
    keys.add(submissionDay);

    DsosFilter filter = new DsosFilter();
    List<GaenKeyForInterops> filteredKeys = filter.filter(UTCInstant.now(), keys);
    assertEquals(3, filteredKeys.size());
  }

  @Test
  public void testDsosUnknown() {
    List<GaenKeyForInterops> keys = new ArrayList<>();

    GaenKeyForInterops submissionMinus4 = new GaenKeyForInterops();
    submissionMinus4.setDaysSinceOnsetOfSymptoms(3996);
    keys.add(submissionMinus4);

    GaenKeyForInterops submissionMinus3 = new GaenKeyForInterops();
    submissionMinus3.setDaysSinceOnsetOfSymptoms(3997);
    keys.add(submissionMinus3);

    GaenKeyForInterops submissionMinus2 = new GaenKeyForInterops();
    submissionMinus2.setDaysSinceOnsetOfSymptoms(3998);
    keys.add(submissionMinus2);

    GaenKeyForInterops submissionMinus1 = new GaenKeyForInterops();
    submissionMinus1.setDaysSinceOnsetOfSymptoms(3999);
    keys.add(submissionMinus1);

    // Submission day
    GaenKeyForInterops submissionDay = new GaenKeyForInterops();
    submissionDay.setDaysSinceOnsetOfSymptoms(4000);
    keys.add(submissionDay);

    DsosFilter filter = new DsosFilter();
    List<GaenKeyForInterops> filteredKeys = filter.filter(UTCInstant.now(), keys);
    assertEquals(3, filteredKeys.size());
  }

  @Test
  public void testDsosSpecificDate() {
    List<GaenKeyForInterops> keys = new ArrayList<>();

    GaenKeyForInterops onsetDayMinus3 = new GaenKeyForInterops();
    onsetDayMinus3.setDaysSinceOnsetOfSymptoms(-3);
    keys.add(onsetDayMinus3);

    GaenKeyForInterops onsetDayMinus2 = new GaenKeyForInterops();
    onsetDayMinus2.setDaysSinceOnsetOfSymptoms(-2);
    keys.add(onsetDayMinus2);

    GaenKeyForInterops onsetDayMinus1 = new GaenKeyForInterops();
    onsetDayMinus1.setDaysSinceOnsetOfSymptoms(-1);
    keys.add(onsetDayMinus1);

    GaenKeyForInterops onsetDay = new GaenKeyForInterops();
    onsetDay.setDaysSinceOnsetOfSymptoms(0);
    keys.add(onsetDay);

    // Submission day
    GaenKeyForInterops submissionDay = new GaenKeyForInterops();
    submissionDay.setDaysSinceOnsetOfSymptoms(1);
    keys.add(submissionDay);

    DsosFilter filter = new DsosFilter();
    List<GaenKeyForInterops> filteredKeys = filter.filter(UTCInstant.now(), keys);
    assertEquals(4, filteredKeys.size());
  }
}
