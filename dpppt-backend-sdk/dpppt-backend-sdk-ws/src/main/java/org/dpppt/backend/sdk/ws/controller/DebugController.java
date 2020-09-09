package org.dpppt.backend.sdk.ws.controller;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import org.dpppt.backend.sdk.data.gaen.DebugGAENDataService;
import org.dpppt.backend.sdk.model.gaen.DayBuckets;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.ClaimIsBeforeOnsetException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.InvalidDateException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.WrongScopeException;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.BadBatchReleaseTimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("/v1/debug")
public class DebugController {
  private final ValidateRequest validateRequest;
  private final ValidationUtils validationUtils;
  private final Duration releaseBucketDuration;
  private final Duration requestTime;
  private final ProtoSignature gaenSigner;
  private final DebugGAENDataService dataService;

  public DebugController(
      DebugGAENDataService dataService,
      ProtoSignature gaenSigner,
      ValidateRequest validateRequest,
      ValidationUtils validationUtils,
      Duration releaseBucketDuration,
      Duration requestTime) {
    this.validateRequest = validateRequest;
    this.validationUtils = validationUtils;
    this.releaseBucketDuration = releaseBucketDuration;
    this.requestTime = requestTime;
    this.gaenSigner = gaenSigner;
    this.dataService = dataService;
  }

  @PostMapping(value = "/exposed")
  public @ResponseBody ResponseEntity<String> addExposed(
      @Valid @RequestBody GaenRequest gaenRequest,
      @RequestHeader(value = "User-Agent", required = true) String userAgent,
      @RequestHeader(value = "X-Device-Name", required = true) String deviceName,
      @AuthenticationPrincipal Object principal)
      throws InvalidDateException, ClaimIsBeforeOnsetException, WrongScopeException {
    var now = UTCInstant.now();
    this.validateRequest.isValid(principal);

    List<GaenKey> nonFakeKeys = new ArrayList<>();
    for (var key : gaenRequest.getGaenKeys()) {
      if (!validationUtils.isValidKeyFormat(key.getKeyData())) {
        return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
      }
      this.validateRequest.validateKeyDate(now, principal, key);
      if (this.validateRequest.isFakeRequest(principal, key)) {
        continue;
      } else {
        nonFakeKeys.add(key);
      }
    }
    if (principal instanceof Jwt
        && ((Jwt) principal).containsClaim("fake")
        && ((Jwt) principal).getClaim("fake").equals("1")
        && !nonFakeKeys.isEmpty()) {
      return ResponseEntity.badRequest().body("Claim is fake but list contains non fake keys");
    }
    if (!nonFakeKeys.isEmpty()) {
      dataService.upsertExposees(deviceName, nonFakeKeys);
    }

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
            batchReleaseTimeDuration.toMillis(), releaseBucketDuration.toMillis());

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
}
