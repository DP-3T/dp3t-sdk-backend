/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.backend.sdk.interops.insertmanager.insertionfilters;

import java.util.List;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;

/**
 * This filter allows to drop/ignore certain keys based on DSOS (Days Since Onset of Symptoms)
 * values when downloading keys from the EFGS hub. The filter itself can be enabled/disabled in the
 * configuration of DP3T. Also the thresholds itself can be defined in the configuration.
 */
public class EfgsDsosFilter implements InteropsKeyInsertionFilter {
  private static int DSOS_RANGE_MAX = 19;

  int symptomaticOnsetKnownDropDaysBeforeOnset;
  int symptomaticOnsetRangeDropDaysBeforeRangeStart;
  int symptomaticUnknownOnsetDropDaysBeforeSubmission;
  int asymptomaticDropDaysBeforeSubmission;
  int unknownSymptomStatusDropDaysBeforeSubmission;

  public EfgsDsosFilter(
      int symptomaticOnsetKnownDropDaysBeforeOnset,
      int symptomaticOnsetRangeDropDaysBeforeRangeStart,
      int symptomaticUnknownOnsetDropDaysBeforeSubmission,
      int asymptomaticDropDaysBeforeSubmission,
      int unknownSymptomStatusDropDaysBeforeSubmission) {

    this.symptomaticOnsetKnownDropDaysBeforeOnset = symptomaticOnsetKnownDropDaysBeforeOnset;
    this.symptomaticOnsetRangeDropDaysBeforeRangeStart =
        symptomaticOnsetRangeDropDaysBeforeRangeStart;
    this.symptomaticUnknownOnsetDropDaysBeforeSubmission =
        symptomaticUnknownOnsetDropDaysBeforeSubmission;
    this.asymptomaticDropDaysBeforeSubmission = asymptomaticDropDaysBeforeSubmission;
    this.unknownSymptomStatusDropDaysBeforeSubmission =
        unknownSymptomStatusDropDaysBeforeSubmission;
  }

  @Override
  public List<GaenKeyForInterops> filter(UTCInstant now, List<GaenKeyForInterops> content) {
    return content.stream().filter(key -> !dropKeyDueToDsosValue(key)).collect(Collectors.toList());
  }

  private boolean dropKeyDueToDsosValue(GaenKeyForInterops key) {
    Integer EFGS_DSOS = key.getDaysSinceOnsetOfSymptoms();
    // keys with dsos null values are dropped
    if (EFGS_DSOS == null) {
      return true;
    }

    if (EFGS_DSOS < 20) {

      // Symptomatic with known onset
      // Range: [-14, +14]
      // Zero point: Key which is matching the exact given Date.

      int daysSinceOnsetOfSymptoms = EFGS_DSOS;
      return daysSinceOnsetOfSymptoms < symptomaticOnsetKnownDropDaysBeforeOnset;

    } else if (EFGS_DSOS < 1986) {

      // Symptomatic with onset range of `n` days (n < 19)
      // Range: [n*100-14, n*100+14]
      // Zero point: Key from yesterday.

      // Detect `n` day rage
      final int n = (EFGS_DSOS + DSOS_RANGE_MAX) / 100;
      // To get an n-day range subtract n-1 nights from `endOfRange`
      int endOfRange = n * 100;
      int startOfRange = endOfRange - (n - 1);

      // Calculate days since start of range
      int daysSinceStartOfRange = EFGS_DSOS - startOfRange;

      return daysSinceStartOfRange < symptomaticOnsetRangeDropDaysBeforeRangeStart;

    } else if (EFGS_DSOS < 2986) {

      // Symptomatic with unknown onset (days since submission)
      // Range: [1986, 2014]
      // Zero point: Key from Today.

      int daysSinceSubmission = EFGS_DSOS - 2000;
      return daysSinceSubmission < symptomaticUnknownOnsetDropDaysBeforeSubmission;

    } else if (EFGS_DSOS < 3986) {

      // Asymptomatic (days since submission)
      // Range: [2986, 3014]
      // Zero point: Key from Today.

      int daysSinceSubmission = EFGS_DSOS - 3000;
      return daysSinceSubmission < asymptomaticDropDaysBeforeSubmission;

    } else {
      // Unknown symptom status (days since submission)
      // Range: [3986, 4014]
      // Zero point: Key from Today.

      int daysSinceSubmission = EFGS_DSOS - 4000;
      return daysSinceSubmission < unknownSymptomStatusDropDaysBeforeSubmission;
    }
  }

  @Override
  public String getName() {
    return "DSOS";
  }
}
