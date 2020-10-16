package org.dpppt.backend.sdk.interops.syncer;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.interops.model.IrishHubDownloadResponse;
import org.dpppt.backend.sdk.interops.model.IrishHubKey;
import org.dpppt.backend.sdk.interops.utils.RestTemplateHelper;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
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
  private final int retentionDays;
  private final GAENDataService gaenDataService;

  // keep batch tags per day.
  private Map<LocalDate, String> lastBatchTag = new HashMap<>();

  // path for downloading keys. the %s must be replaced by day dates to retreive the keys for one
  // day, for example: 2020-09-15
  private static final String DOWNLOAD_PATH = "/diagnosiskeys/download/%s";

  private static final String UPLOAD_PATH = "/diagnosiskeys/upload";

  private final RestTemplate rt = RestTemplateHelper.getRestTemplate();
  private static final Logger logger = LoggerFactory.getLogger(IrishHubSyncer.class);

  public IrishHubSyncer(
      String baseUrl,
      String authorizationToken,
      int retentionDays,
      GAENDataService gaenDataService) {
    this.baseUrl = baseUrl;
    this.authorizationToken = authorizationToken;
    this.retentionDays = retentionDays;
    this.gaenDataService = gaenDataService;
  }

  public void sync() {
    long start = System.currentTimeMillis();
    logger.info("Start sync from: " + baseUrl);
    LocalDate today = LocalDate.now();
    try {
      download(today);

    } catch (Exception e) {
      logger.error("Exception downloading keys:", e);
    }

    long end = System.currentTimeMillis();
    logger.info("Sync done in: " + (end - start) + " [ms]");
  }

  private void download(LocalDate today) throws URISyntaxException {
    LocalDate endDate = today.minusDays(retentionDays);
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
            RequestEntity.get(builder.build().toUri())
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
            gaenKey, now, irishKey.getOrigin(), irishKey.getRegions());
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

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + authorizationToken);
    return headers;
  }
}
