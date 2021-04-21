/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.interops.syncer.efgs;

import static org.dpppt.backend.sdk.utils.EfgsDsosUtil.calculateDefaultDsosMapping;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.dpppt.backend.sdk.interops.model.EfgsBatchUploadResponse;
import org.dpppt.backend.sdk.interops.model.EfgsGatewayConfig;
import org.dpppt.backend.sdk.interops.model.GaenKeyBatch;
import org.dpppt.backend.sdk.interops.syncer.efgs.signing.BatchSigner;
import org.dpppt.backend.sdk.interops.syncer.efgs.signing.CryptoProvider;
import org.dpppt.backend.sdk.interops.utils.RestTemplateHelper;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.model.gaen.ReportType;
import org.dpppt.backend.sdk.model.interops.proto.EfgsProto;
import org.dpppt.backend.sdk.model.interops.proto.EfgsProto.DiagnosisKey;
import org.dpppt.backend.sdk.model.interops.proto.EfgsProto.DiagnosisKeyBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class EfgsClient {
  private static final Logger logger = LoggerFactory.getLogger(EfgsClient.class);

  private final String gatewayId;
  private final String baseUrl;
  private final RestTemplate rt;
  private final BatchSigner signer;
  private final List<String> visitedCountries;
  private final Integer defaultTransmissionRiskLevel;
  private final ReportType defaultReportType;

  private static final String UPLOAD_PATH = "/diagnosiskeys/upload";
  private static final String DOWNLOAD_PATH = "/diagnosiskeys/download/%s";

  public EfgsClient(EfgsGatewayConfig efgsGatewayConfig)
      throws CertificateException, UnrecoverableKeyException, KeyStoreException,
          NoSuchAlgorithmException, IOException {
    this.gatewayId = efgsGatewayConfig.getId();
    this.baseUrl = efgsGatewayConfig.getBaseUrl();
    this.rt =
        RestTemplateHelper.getRestTemplateWithClientCerts(
            efgsGatewayConfig.getAuthClientCert(),
            efgsGatewayConfig.getAuthClientCertPassword(),
            List.of(
                UriComponentsBuilder.fromHttpUrl(efgsGatewayConfig.getBaseUrl())
                    .build()
                    .toUri()
                    .getHost()));
    this.signer =
        new BatchSigner(
            new CryptoProvider(
                efgsGatewayConfig.getSignClientCert(),
                efgsGatewayConfig.getSignClientCertPassword()),
            efgsGatewayConfig.getSignAlgorithmName());
    this.visitedCountries = efgsGatewayConfig.getVisitedCountries();
    this.defaultTransmissionRiskLevel = efgsGatewayConfig.getDefaultTransmissionRiskLevel();
    this.defaultReportType = efgsGatewayConfig.getDefaultReportType();
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getGatewayId() {
    return gatewayId;
  }

  public GaenKeyBatch download(LocalDate date, String batchTag) {
    GaenKeyBatch keyBatch = new GaenKeyBatch(date);
    URI uri =
        UriComponentsBuilder.fromHttpUrl(
                baseUrl
                    + String.format(
                        DOWNLOAD_PATH, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .build()
            .toUri();
    RequestEntity<Void> request =
        RequestEntity.get(uri).headers(createDownloadHeaders(batchTag)).build();
    try {
      logger.info("downloading keys for date: {} batchTag: {}", date, batchTag);
      ResponseEntity<byte[]> response = rt.exchange(request, byte[].class);
      keyBatch.setBatchTag(response.getHeaders().get("batchTag").get(0));
      keyBatch.setNextBatchTag(response.getHeaders().get("nextBatchTag").get(0));
      try {
        if (response.getBody() != null) {
          DiagnosisKeyBatch diagnosisKeyBatch = DiagnosisKeyBatch.parseFrom(response.getBody());
          keyBatch.setKeys(mapToGaenKeyForInteropsList(diagnosisKeyBatch));
        }
      } catch (Exception e) {
        logger.error(
            "unable to parse downloaded DiagnosisKeyBatch protobuf for date: {} batchTag: {}",
            date,
            batchTag);
      }
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        logger.info("no keys to download for date: {}", date);
      } else if (e.getStatusCode().equals(HttpStatus.GONE)) {
        logger.info("date: {} is too far in the past", date);
      } else {
        throw e;
      }
    }
    return keyBatch;
  }

  /**
   * @param batchToUpload
   * @param batchTag
   * @throws OperatorCreationException
   * @throws GeneralSecurityException
   * @throws CMSException
   * @throws IOException
   * @return uploaded keys
   */
  public List<GaenKeyForInterops> upload(List<GaenKeyForInterops> batchToUpload, String batchTag)
      throws OperatorCreationException, GeneralSecurityException, CMSException, IOException {
    DiagnosisKeyBatch efgsBatch = mapToEfgsBatch(batchToUpload);

    ResponseEntity<EfgsBatchUploadResponse> response = doUploadPostRequest(batchTag, efgsBatch);

    if (response.getStatusCode().equals(HttpStatus.CREATED)) {
      logger.info(
          "batchTag '{}': all {} key(s) successfully uploaded", batchTag, batchToUpload.size());
      return batchToUpload;
    } else if (response.getStatusCode().equals(HttpStatus.MULTI_STATUS)) {
      logger.info(
          "batchTag '{}': {} key(s) successfully uploaded",
          batchTag,
          response.getBody().getStatus201().size());
      logger.info(
          "batchTag '{}': {} key(s) already uploaded previously",
          batchTag,
          response.getBody().getStatus409().size());
      List<Integer> failedIndexes = response.getBody().getStatus500();
      logger.warn("batchTag '{}': {} key(s) failed to upload", batchTag, failedIndexes.size());
      return getUploadedKeys(batchToUpload, failedIndexes);
    } else {
      throw new RuntimeException("unexpected statuscode returned: " + response.getStatusCode());
    }
  }

  private ResponseEntity<EfgsBatchUploadResponse> doUploadPostRequest(
      String batchTag, DiagnosisKeyBatch efgsBatch)
      throws GeneralSecurityException, CMSException, OperatorCreationException, IOException {
    URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + UPLOAD_PATH).build().toUri();
    RequestEntity<Object> request =
        RequestEntity.post(uri)
            .headers(createUploadHeaders(batchTag, signer.createSignatureBytes(efgsBatch)))
            .body(efgsBatch.toByteArray());
    return rt.exchange(request, EfgsBatchUploadResponse.class);
  }

  private List<GaenKeyForInterops> getUploadedKeys(
      List<GaenKeyForInterops> batchToUpload, List<Integer> failedIndexes) {
    List<GaenKeyForInterops> uploadedKeys = new ArrayList<>();
    for (int i = 0; i < batchToUpload.size(); i++) {
      if (!failedIndexes.contains(i)) {
        uploadedKeys.add(batchToUpload.get(i));
      }
    }
    return uploadedKeys;
  }

  private HttpHeaders createDownloadHeaders(String lastBatchTag) {
    HttpHeaders headers = new HttpHeaders();
    if (lastBatchTag != null) {
      headers.add("batchTag", lastBatchTag);
    }
    headers.add(HttpHeaders.ACCEPT, "application/protobuf; version=1.0");
    return headers;
  }

  private HttpHeaders createUploadHeaders(String batchTag, String batchSignature) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("batchTag", batchTag);
    headers.add("batchSignature", batchSignature);
    headers.add(HttpHeaders.CONTENT_TYPE, "application/protobuf; version=1.0");
    return headers;
  }

  private DiagnosisKeyBatch mapToEfgsBatch(List<GaenKeyForInterops> batchToUpload) {
    return DiagnosisKeyBatch.newBuilder()
        .addAllKeys(batchToUpload.stream().map(k -> mapToEfgsKey(k)).collect(Collectors.toList()))
        .build();
  }

  private DiagnosisKey mapToEfgsKey(GaenKeyForInterops gaenKey) {
    return EfgsProto.DiagnosisKey.newBuilder()
        .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode(gaenKey.getKeyData())))
        .setRollingStartIntervalNumber(gaenKey.getRollingStartNumber())
        .setRollingPeriod(gaenKey.getRollingPeriod())
        .setTransmissionRiskLevel(defaultTransmissionRiskLevel)
        .addAllVisitedCountries(visitedCountries)
        .setOrigin(gaenKey.getOrigin())
        .setReportType(
            gaenKey.getReportType() != null
                ? gaenKey.getReportType().toEfgsProtoReportType()
                : defaultReportType.toEfgsProtoReportType())
        .setDaysSinceOnsetOfSymptoms(
            gaenKey.getDaysSinceOnsetOfSymptoms() != null
                ? gaenKey.getDaysSinceOnsetOfSymptoms()
                : calculateDefaultDsosMapping(gaenKey))
        .build();
  }

  private List<GaenKeyForInterops> mapToGaenKeyForInteropsList(
      DiagnosisKeyBatch diagnosisKeyBatch) {
    return diagnosisKeyBatch.getKeysList().stream()
        .map(k -> mapToGaenKeyForInterops(k))
        .collect(Collectors.toList());
  }

  private GaenKeyForInterops mapToGaenKeyForInterops(DiagnosisKey diagnosisKey) {
    GaenKeyForInterops keyForInterops = new GaenKeyForInterops();
    keyForInterops.setGaenKey(new GaenKey());
    keyForInterops.setKeyData(
        java.util.Base64.getEncoder().encodeToString(diagnosisKey.getKeyData().toByteArray()));
    keyForInterops.setRollingStartNumber(diagnosisKey.getRollingStartIntervalNumber());
    keyForInterops.setRollingPeriod(diagnosisKey.getRollingPeriod());
    keyForInterops.setTransmissionRiskLevel(diagnosisKey.getTransmissionRiskLevel());
    keyForInterops.setFake(0);
    keyForInterops.setOrigin(diagnosisKey.getOrigin());
    keyForInterops.setReportType(ReportType.fromEfgsProtoReportType(diagnosisKey.getReportType()));
    keyForInterops.setDaysSinceOnsetOfSymptoms(diagnosisKey.getDaysSinceOnsetOfSymptoms());
    return keyForInterops;
  }
}
