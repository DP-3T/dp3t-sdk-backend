/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.interops.insertmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.interops.insertmanager.insertionfilters.InteropsKeyInsertionFilter;
import org.dpppt.backend.sdk.interops.insertmanager.insertionmodifier.InteropsKeyInsertionModifier;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The insertion manager is responsible for inserting keys downloaded from an international
 * gateway/hub into the database. To make sure we only have valid keys in the database, a list of
 * {@link InteropsKeyInsertionModifier} is applied, and then a list of {@link
 * InteropsKeyInsertionFilter} is applied to the given list of keys. The remaining keys are then
 * inserted into the database.
 */
public class InteropsInsertManager {

  private final List<InteropsKeyInsertionFilter> filterList = new ArrayList<>();
  private final List<InteropsKeyInsertionModifier> modifierList = new ArrayList<>();

  private final GaenDataService dataService;

  private static final Logger logger = LoggerFactory.getLogger(InteropsInsertManager.class);

  public InteropsInsertManager(GaenDataService dataService) {
    this.dataService = dataService;
  }

  public void addFilter(InteropsKeyInsertionFilter filter) {
    this.filterList.add(filter);
  }

  public void addModifier(InteropsKeyInsertionModifier modifier) {
    this.modifierList.add(modifier);
  }

  /**
   * Inserts the keys into the database. The additional parameters are supplied to the configured
   * modifiers and filters.
   *
   * @param keys the list of downloaded international keys
   * @param now current timestamp to work with.
   * @param batchTag
   */
  public void insertIntoDatabase(List<GaenKeyForInterops> keys, UTCInstant now, String batchTag) {
    if (keys == null || keys.isEmpty()) {
      return;
    }
    var internalKeys = modifyAndFilter(keys, now);
    if (!internalKeys.isEmpty()) {
      for (Entry<String, List<GaenKeyForInterops>> keysForOrigin :
          internalKeys.stream()
              .collect(Collectors.groupingBy(GaenKeyForInterops::getOrigin))
              .entrySet()) {
        dataService.upsertExposeeFromInterops(
            keysForOrigin.getValue().stream()
                .map(GaenKeyForInterops::getGaenKey)
                .collect(Collectors.toList()),
            now,
            keysForOrigin.getKey(),
            batchTag);
      }
    }
  }

  private List<GaenKeyForInterops> modifyAndFilter(List<GaenKeyForInterops> keys, UTCInstant now) {
    var internalKeys = keys;

    for (InteropsKeyInsertionModifier modifier : modifierList) {
      internalKeys = modifier.modify(now, internalKeys);
    }

    for (InteropsKeyInsertionFilter filter : filterList) {
      int sizeBefore = internalKeys.size();
      internalKeys = filter.filter(now, internalKeys);
      logger.info(
          "{} keys filtered out by {} filter",
          (internalKeys.size() - sizeBefore),
          filter.getName());
    }
    return internalKeys;
  }
}
