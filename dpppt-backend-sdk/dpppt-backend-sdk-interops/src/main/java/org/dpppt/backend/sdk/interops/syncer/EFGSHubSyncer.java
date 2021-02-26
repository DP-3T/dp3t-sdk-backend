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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.interops.syncer.efgs.EFGSClient;
import org.dpppt.backend.sdk.interops.utils.RestTemplateHelper;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Interops syncer for the EFGS HUB:
 * https://github.com/eu-federation-gateway-service/efgs-federation-gateway
 *
 * @author alig
 */
public class EFGSHubSyncer {

  private final EFGSClient efgsClient;

  private final Duration retentionPeriod;
  private final Duration releaseBucketDuration;

  private final GAENDataService gaenDataService;
  private final String origin;

  // keep uploaded till timestamp
  private UTCInstant lastUploadTill = UTCInstant.ofEpochMillis(0l);

  private final RestTemplate rt = RestTemplateHelper.getRestTemplate();
  private static final Logger logger = LoggerFactory.getLogger(EFGSHubSyncer.class);

  public EFGSHubSyncer(
      EFGSClient efgsClient,
      Duration retentionPeriod,
      Duration releaseBucketDuration,
      GAENDataService gaenDataService,
      String origin) {
    this.retentionPeriod = retentionPeriod;
    this.releaseBucketDuration = releaseBucketDuration;
    this.gaenDataService = gaenDataService;
    this.origin = origin;
    this.efgsClient = efgsClient;
  }

  public void sync() {
    long start = System.currentTimeMillis();
    logger.info("Start sync from: " + efgsClient.getBaseUrl());
    UTCInstant now = UTCInstant.now();
    LocalDate today = now.getLocalDate();
    try {
      download(today);
    } catch (Exception e) {
      logger.error("Exception downloading keys:", e);
    }
    try {
      upload(now);
    } catch (Exception e) {
      logger.error("Exception uploading keys:", e);
    }

    long end = System.currentTimeMillis();
    logger.info("Sync done in: " + (end - start) + " [ms]");
  }

  private void upload(UTCInstant now) throws JOSEException, JsonProcessingException {
    logger.info("Start upload keys since: " + lastUploadTill);

    logger.info("Upload done");
  }

  private void download(LocalDate today) throws URISyntaxException {
    logger.info("Start download keys");

    logger.info("Download done");
  }
}
