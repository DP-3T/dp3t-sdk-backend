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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.interops.model.IrishHubDownloadResponse;
import org.dpppt.backend.sdk.interops.model.IrishHubKey;
import org.dpppt.backend.sdk.interops.model.IrishHubUploadResponse;
import org.dpppt.backend.sdk.interops.utils.RestTemplateHelper;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Interops syncer for the irish hub:
 * https://github.com/HSEIreland/covid-green-interoperability-service
 *
 * @author alig
 */
public class IrishHubSyncer {

  private final String baseUrl;
  private final String authorizationToken;
  private final String privateKeyPem;
  private final Duration retentionPeriod;
  private final Duration releaseBucketDuration;

  private final GaenDataService gaenDataService;
  private final String origin;

  // keep batch tags per day.
  private Map<LocalDate, String> lastBatchTag = new HashMap<>();

  // keep uploaded till timestamp
  private UTCInstant lastUploadTill = UTCInstant.ofEpochMillis(0l);

  private final ObjectMapper om = new ObjectMapper();

  // path for downloading keys. the %s must be replaced by day dates to retreive the keys for one
  // day, for example: 2020-09-15
  private static final String DOWNLOAD_PATH = "/diagnosiskeys/download/%s";

  private static final String UPLOAD_PATH = "/diagnosiskeys/upload";

  private final RestTemplate rt = RestTemplateHelper.getRestTemplate();
  private static final Logger logger = LoggerFactory.getLogger(IrishHubSyncer.class);

  public IrishHubSyncer(
      String baseUrl,
      String authorizationToken,
      String privateKeyPem,
      Duration retentionPeriod,
      Duration releaseBucketDuration,
      GaenDataService gaenDataService,
      String origin) {
    this.baseUrl = baseUrl;
    this.authorizationToken = authorizationToken;
    this.retentionPeriod = retentionPeriod;
    this.releaseBucketDuration = releaseBucketDuration;
    this.gaenDataService = gaenDataService;
    this.privateKeyPem = privateKeyPem;
    this.origin = origin;
  }

  public void sync() {
    long start = System.currentTimeMillis();
    logger.info("Start sync from: " + baseUrl);
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
    UTCInstant uploadTill = now.roundToBucketStart(releaseBucketDuration);
    List<GaenKeyForInterops> keysToUpload =
        gaenDataService.getSortedExposedSinceForInteropsFromOrigin(lastUploadTill, now);
    logger.info("Found " + keysToUpload.size() + " keys to upload");

    List<IrishHubKey> irishKeysToUpload = new ArrayList<>();
    for (GaenKeyForInterops gaenKey : keysToUpload) {
      irishKeysToUpload.add(mapToIrishKey(gaenKey));
    }

    JWK jwk = JWK.parseFromPEMEncodedObjects(privateKeyPem);
    JWSSigner signer = new RSASSASigner(jwk.toRSAKey());
    JWSObject jwsObject =
        new JWSObject(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(jwk.getKeyID()).build(),
            new Payload(om.writeValueAsBytes(irishKeysToUpload)));
    jwsObject.sign(signer);
    String payload = jwsObject.serialize();

    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + UPLOAD_PATH);
    URI uri = builder.build().toUri();
    RequestEntity<Object> request =
        RequestEntity.post(uri)
            .headers(createHeaders())
            .body(createUploadBody(payload, uploadTill));
    ResponseEntity<IrishHubUploadResponse> response =
        rt.exchange(request, IrishHubUploadResponse.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      logger.info(
          "Upload success. BatchTag: "
              + response.getBody().getBatchTag()
              + " Inserted keys: "
              + response.getBody().getInsertedExposures());
    }
    logger.info("Upload done");
  }

  private MultiValueMap<String, String> createUploadBody(String payload, UTCInstant uploadTill) {
    String batchTag = DigestUtils.sha256Hex(String.valueOf(uploadTill.getTimestamp()) + origin);
    MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
    body.add("batchTag", batchTag);
    body.add("payload", payload);
    return body;
  }

  private void download(LocalDate today) throws URISyntaxException {
    LocalDate endDate = today.atStartOfDay().minus(retentionPeriod).toLocalDate();
    LocalDate dayDate = endDate;
    logger.info("Start download: " + endDate + " - " + today);
    List<IrishHubKey> receivedKeys = new ArrayList<>();
    while (dayDate.isBefore(today.plusDays(1))) {
      String lastBatchTagForDay = lastBatchTag.get(dayDate);
      logger.info(
          "Download keys for: "
              + dayDate
              + " BatchTag: "
              + (lastBatchTagForDay != null ? lastBatchTagForDay : " none"));

      boolean done = false;

      while (!done) {
        UriComponentsBuilder builder =
            UriComponentsBuilder.fromHttpUrl(
                baseUrl
                    + String.format(
                        DOWNLOAD_PATH, dayDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        if (lastBatchTagForDay != null) {
          builder.queryParam("batchTag", lastBatchTagForDay);
        }
        URI uri = builder.build().toUri();
        logger.info("Request key for: " + uri.toString());
        RequestEntity<Void> request =
            RequestEntity.get(uri)
                .accept(MediaType.APPLICATION_JSON)
                .headers(createHeaders())
                .build();
        ResponseEntity<IrishHubDownloadResponse> response =
            rt.exchange(request, IrishHubDownloadResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
          if (response.getStatusCode().equals(HttpStatus.OK)) {
            IrishHubDownloadResponse downloadResponse = response.getBody();
            logger.info(
                "Got 200. BatchTag: "
                    + downloadResponse.getBatchTag()
                    + " Number of keys: "
                    + downloadResponse.getExposures().size());
            lastBatchTagForDay = downloadResponse.getBatchTag();
            receivedKeys.addAll(downloadResponse.getExposures());
          } else if (response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            logger.info("Got 204. Store last batch tag");
            // no more keys to load. store last batch tag for next sync
            this.lastBatchTag.put(dayDate, lastBatchTagForDay);
            done = true;
          }
        }
      }
      dayDate = dayDate.plusDays(1);
    }

    UTCInstant now = UTCInstant.now();
    logger.info("Received " + receivedKeys.size() + " keys. Store ...");
    for (IrishHubKey irishKey : receivedKeys) {
      GaenKey gaenKey = mapToGaenKey(irishKey);
      if (irishKey.getOrigin() != null
          && !irishKey.getOrigin().isBlank()
          && !irishKey.getRegions().isEmpty()) {
        gaenDataService.upsertExposeeFromInterops(
            List.of(gaenKey), now, irishKey.getOrigin(), null);
      }
    }
  }

  private GaenKey mapToGaenKey(IrishHubKey irishKey) {
    GaenKey gaenKey = new GaenKey();
    gaenKey.setKeyData(irishKey.getKeyData());
    gaenKey.setRollingPeriod(irishKey.getRollingPeriod());
    gaenKey.setRollingStartNumber(irishKey.getRollingStartNumber());
    return gaenKey;
  }

  private IrishHubKey mapToIrishKey(GaenKeyForInterops gaenKey) {
    IrishHubKey irishHubKey = new IrishHubKey();
    irishHubKey.setKeyData(gaenKey.getKeyData());
    irishHubKey.setOrigin(gaenKey.getOrigin());
    irishHubKey.setRollingPeriod(gaenKey.getRollingPeriod());
    irishHubKey.setRollingStartNumber(gaenKey.getRollingPeriod());
    irishHubKey.setTransmissionRiskLevel(0); // must be set, otherwise fail on upload
    return irishHubKey;
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + authorizationToken);
    return headers;
  }
}
