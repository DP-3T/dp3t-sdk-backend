/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.backend.sdk.interops.syncer;

import com.google.common.collect.Lists;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.codec.binary.Base64;
import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.data.interops.SyncLogDataService;
import org.dpppt.backend.sdk.interops.insertmanager.InteropsInsertManager;
import org.dpppt.backend.sdk.interops.model.GaenKeyBatch;
import org.dpppt.backend.sdk.interops.syncer.efgs.EfgsClient;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.model.interops.FederationSyncLogEntry;
import org.dpppt.backend.sdk.model.interops.SyncAction;
import org.dpppt.backend.sdk.model.interops.SyncState;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interops syncer for the EFGS HUB:
 * https://github.com/eu-federation-gateway-service/efgs-federation-gateway
 *
 * @author alig
 */
public class EfgsHubSyncer {
  private static final Logger logger = LoggerFactory.getLogger(EfgsHubSyncer.class);

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private static int MAX_UPLOAD_BATCH_SIZE = 4000;

  private final EfgsClient efgsClient;
  private final Duration retentionPeriod;
  private final GaenDataService gaenDataService;
  private final SyncLogDataService syncLogDataService;
  private final InteropsInsertManager insertManager;

  public EfgsHubSyncer(
      EfgsClient efgsClient,
      Duration retentionPeriod,
      GaenDataService gaenDataService,
      SyncLogDataService syncLogDataService,
      InteropsInsertManager interopsInsertManager) {
    this.efgsClient = efgsClient;
    this.retentionPeriod = retentionPeriod;
    this.gaenDataService = gaenDataService;
    this.syncLogDataService = syncLogDataService;
    this.insertManager = interopsInsertManager;
  }

  public void sync() {
    long start = System.currentTimeMillis();
    logger.info("Start sync from: " + efgsClient.getBaseUrl());
    LocalDate today = UTCInstant.today().getLocalDate();
    try {
      download(today);
    } catch (Exception e) {
      logger.error("Exception downloading keys:", e);
    }
    try {
      upload();
    } catch (Exception e) {
      logger.error("Exception uploading keys:", e);
    }

    long end = System.currentTimeMillis();
    logger.info("Sync done in: " + (end - start) + " [ms]");
  }

  private void upload() {
    logger.info("Start upload");

    List<GaenKeyForInterops> keysToUpload = gaenDataService.getExposedForEfgsUpload();
    logger.info("Found " + keysToUpload.size() + " keys to upload");

    byte[] hash = new byte[4];
    SECURE_RANDOM.nextBytes(hash);
    AtomicInteger batchCounter = new AtomicInteger(0);
    for (List<GaenKeyForInterops> batchToUpload :
        Lists.partition(keysToUpload, MAX_UPLOAD_BATCH_SIZE)) {
      UTCInstant start = UTCInstant.now();
      String batchTag = generateBatchTag(batchCounter.getAndIncrement(), hash);

      boolean success = true;
      try {
        logger.info("uploading batch (size: {}) with batchTag: {}", batchToUpload.size(), batchTag);
        List<GaenKeyForInterops> uploadedKeys = efgsClient.upload(batchToUpload, batchTag);
        gaenDataService.setBatchTagForKeys(uploadedKeys, batchTag);
      } catch (Exception e) {
        logger.error("Exception uploading keys:", e);
        success = false;
      } finally {
        logUpload(start, batchTag, success);
      }
    }
    logger.info("Upload done");
  }

  public void download(LocalDate today) {
    LocalDate date = today.atStartOfDay().minus(retentionPeriod).toLocalDate();
    logger.info("Start download: " + date + " - " + today);
    while (date.isBefore(today.plusDays(1))) {
      String nextBatchTag = syncLogDataService.getLatestBatchTagForDay(date);
      boolean dayComplete = false;
      final long emergencyBreak = 100000L;
      long loopCounter = 0L;
      while (!dayComplete && loopCounter < emergencyBreak) {
        UTCInstant start = UTCInstant.now();
        GaenKeyBatch keyBatch = efgsClient.download(date, nextBatchTag);
        String downloadedBatchTag = keyBatch.getBatchTag();
        dayComplete = keyBatch.isLastBatchForDay();
        if (!keyBatch.getKeys().isEmpty()) {
          logger.info("upserting downloaded batch: {}", keyBatch);
          insertManager.insertIntoDatabase(
              keyBatch.getKeys(), UTCInstant.now(), downloadedBatchTag);
          logDownload(start, date, downloadedBatchTag, true);
        } else if (downloadedBatchTag != null) {
          logger.info("empty batch: {}", downloadedBatchTag);
          logDownload(start, date, downloadedBatchTag, true);
        }
        nextBatchTag = keyBatch.getNextBatchTag();
        loopCounter++;
      }
      if (loopCounter == emergencyBreak) {
        logger.error("check break condition, emergency break pulled for date: {}", date);
      }
      date = date.plusDays(1);
    }
    logger.info("Download done");
  }

  private String generateBatchTag(int counter, byte[] runnerHash) {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    return String.format(
        "%d-%d-%d-%s-%d",
        now.getYear(),
        now.getMonth().getValue(),
        now.getDayOfMonth(),
        Base64.encodeBase64String(runnerHash),
        counter);
  }

  private void logUpload(UTCInstant start, String batchTag, boolean success) {
    FederationSyncLogEntry logEntry = new FederationSyncLogEntry();
    logEntry.setGateway(efgsClient.getGatewayId());
    logEntry.setAction(SyncAction.UPLOAD);
    logEntry.setBatchTag(batchTag);
    logEntry.setStartTime(start);
    logEntry.setUploadDate(start.getLocalDate());
    logEntry.setEndTime(UTCInstant.now());
    logEntry.setState(success ? SyncState.DONE : SyncState.ERROR);
    syncLogDataService.insertLogEntry(logEntry);
  }

  private void logDownload(
      UTCInstant start, LocalDate date, String latestBatchTagForDay, boolean success) {
    FederationSyncLogEntry logEntry = new FederationSyncLogEntry();
    logEntry.setGateway(efgsClient.getGatewayId());
    logEntry.setAction(SyncAction.DOWNLOAD);
    logEntry.setBatchTag(latestBatchTagForDay);
    logEntry.setUploadDate(date);
    logEntry.setStartTime(start);
    logEntry.setEndTime(UTCInstant.now());
    logEntry.setState(success ? SyncState.DONE : SyncState.ERROR);
    syncLogDataService.insertLogEntry(logEntry);
  }
}
