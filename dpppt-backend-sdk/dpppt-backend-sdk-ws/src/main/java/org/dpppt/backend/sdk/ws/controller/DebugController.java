package org.dpppt.backend.sdk.ws.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import javax.validation.Valid;
import org.dpppt.backend.sdk.data.gaen.DebugGaenDataService;
import org.dpppt.backend.sdk.model.gaen.DayBuckets;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.InsertException;
import org.dpppt.backend.sdk.ws.insertmanager.InsertManager;
import org.dpppt.backend.sdk.ws.insertmanager.insertionfilters.AssertKeyFormat.KeyFormatException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.ClaimIsBeforeOnsetException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.InvalidDateException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.WrongScopeException;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.BadBatchReleaseTimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("/v1/debug")
public class DebugController {
  private final ValidateRequest validateRequest;
  private final InsertManager insertManager;
  private final Duration releaseBucketDuration;
  private final Duration requestTime;
  private final ProtoSignature gaenSigner;
  private final DebugGaenDataService dataService;

  public DebugController(
      DebugGaenDataService dataService,
      ProtoSignature gaenSigner,
      ValidateRequest validateRequest,
      InsertManager insertManager,
      Duration releaseBucketDuration,
      Duration requestTime) {
    this.validateRequest = validateRequest;
    this.releaseBucketDuration = releaseBucketDuration;
    this.requestTime = requestTime;
    this.gaenSigner = gaenSigner;
    this.dataService = dataService;
    this.insertManager = insertManager;
  }

  @PostMapping(value = "/exposed")
  public @ResponseBody ResponseEntity<String> addExposed(
      @Valid @RequestBody GaenRequest gaenRequest,
      @RequestHeader(value = "User-Agent", required = true) String userAgent,
      @RequestHeader(value = "X-Device-Name", required = true) String deviceName,
      @AuthenticationPrincipal Object principal)
      throws WrongScopeException, InsertException {
    var now = UTCInstant.now();
    this.validateRequest.isValid(principal);

    // Filter out non valid keys and insert them into the database (c.f. InsertManager and
    // configured Filters in the WSBaseConfig)
    insertManager.insertIntoDatabaseDEBUG(
        deviceName, gaenRequest.getGaenKeys(), userAgent, principal, now);
    var responseBuilder = ResponseEntity.ok();

    normalizeRequestTime(now.getTimestamp());
    return responseBuilder.build();
  }

  @GetMapping(value = "/exposed/{batchReleaseTime}", produces = "application/zip")
  public @ResponseBody ResponseEntity<byte[]> getExposedKeys(
      @PathVariable long batchReleaseTime, WebRequest request)
      throws BadBatchReleaseTimeException, IOException, InvalidKeyException,
          NoSuchAlgorithmException, SignatureException {

    var batchReleaseTimeDuration = Duration.ofMillis(batchReleaseTime);

    if (batchReleaseTime % releaseBucketDuration.toMillis() != 0) {
      return ResponseEntity.notFound().build();
    }

    var exposedKeys =
        dataService.getSortedExposedForBatchReleaseTime(
            UTCInstant.ofEpochMillis(batchReleaseTime), releaseBucketDuration);

    byte[] payload = gaenSigner.getPayload(exposedKeys);

    return ResponseEntity.ok()
        .header("X-BATCH-RELEASE-TIME", Long.toString(batchReleaseTimeDuration.toMillis()))
        .body(payload);
  }

  @GetMapping(value = "/buckets/{dayDateStr}")
  public @ResponseBody ResponseEntity<DayBuckets> getBuckets(@PathVariable String dayDateStr) {
    var atStartOfDay = UTCInstant.parseDate(dayDateStr);
    var end = atStartOfDay.plusDays(1);
    var now = UTCInstant.now();
    // if (!validationUtils.isDateInRange(atStartOfDay)) {
    //     return ResponseEntity.notFound().build();
    // }
    var relativeUrls = new ArrayList<String>();
    var dayBuckets = new DayBuckets();

    String controllerMapping = this.getClass().getAnnotation(RequestMapping.class).value()[0];
    dayBuckets.setDay(dayDateStr).setRelativeUrls(relativeUrls);

    while (atStartOfDay.getTimestamp() < Math.min(now.getTimestamp(), end.getTimestamp())) {
      relativeUrls.add(controllerMapping + "/exposed" + "/" + atStartOfDay.getTimestamp());
      atStartOfDay = atStartOfDay.plus(this.releaseBucketDuration);
    }

    return ResponseEntity.ok(dayBuckets);
  }

  private void normalizeRequestTime(long now) {
    long after = UTCInstant.now().getTimestamp();
    long duration = after - now;
    try {
      Thread.sleep(Math.max(requestTime.minusMillis(duration).toMillis(), 0));
    } catch (Exception ex) {

    }
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    InvalidDateException.class,
    JsonProcessingException.class,
    MethodArgumentNotValidException.class,
    BadBatchReleaseTimeException.class,
    DateTimeParseException.class,
    ClaimIsBeforeOnsetException.class,
    KeyFormatException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Object> invalidArguments() {
    return ResponseEntity.badRequest().build();
  }

  @ExceptionHandler({WrongScopeException.class})
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ResponseEntity<Object> forbidden() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }
}
