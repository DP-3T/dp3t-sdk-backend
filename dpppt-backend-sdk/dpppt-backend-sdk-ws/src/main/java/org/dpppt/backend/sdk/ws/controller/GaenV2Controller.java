package org.dpppt.backend.sdk.ws.controller;

import ch.ubique.openapi.docannotations.Documentation;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.validation.Valid;
import org.dpppt.backend.sdk.data.gaen.FakeKeyService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenV2UploadKeysRequest;
import org.dpppt.backend.sdk.utils.DurationExpiredException;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.insertmanager.InsertException;
import org.dpppt.backend.sdk.ws.insertmanager.InsertManager;
import org.dpppt.backend.sdk.ws.insertmanager.insertionfilters.AssertKeyFormat.KeyFormatException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.ClaimIsBeforeOnsetException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.InvalidDateException;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.WrongScopeException;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature.ProtoSignatureWrapper;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.BadBatchReleaseTimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This is a new controller to simplify the sending and receiving of keys. It will be used by the
 * new SwissCovid client and allows for cross-country usage.
 */
@Controller
@RequestMapping("/v2/gaen")
@Documentation(
    description =
        "The GAEN V2 endpoint for the mobile clients supporting international key sharing")
public class GaenV2Controller {

  /**
   * This API includes the implicit design decision that the list of visited countries * applies to
   * all keys (rather than per key). * This design decision makes sense to us for two reasons: * 1.
   * Detailed per-day travel information is not available from the current UI * 2. For simplicity
   * the UI should probably also not aim to request this information
   */
  private static final Logger logger = LoggerFactory.getLogger(GaenV2Controller.class);

  private final InsertManager insertManager;

  private final ValidateRequest validateRequest;

  private final ValidationUtils validationUtils;
  private final FakeKeyService fakeKeyService;
  private final ProtoSignature gaenSigner;
  private final Duration releaseBucketDuration;
  private final Duration requestTime;
  private final Duration exposedListCacheControl;

  public GaenV2Controller(
      InsertManager insertManager,
      ValidateRequest validateRequest,
      ValidationUtils validationUtils,
      FakeKeyService fakeKeyService,
      ProtoSignature gaenSigner,
      Duration releaseBucketDuration,
      Duration requestTime,
      Duration exposedListCacheControl) {
    this.insertManager = insertManager;
    this.validateRequest = validateRequest;
    this.validationUtils = validationUtils;
    this.fakeKeyService = fakeKeyService;
    this.gaenSigner = gaenSigner;
    this.releaseBucketDuration = releaseBucketDuration;
    this.requestTime = requestTime;
    this.exposedListCacheControl = exposedListCacheControl;
  }

  @PostMapping(value = "/exposed")
  @Documentation(
      description =
          "Endpoint used to upload exposure keys to the backend specifying for which countries the"
              + " keys are valid.",
      responses = {
        "200=>The exposed keys have been stored in the database",
        "400=> "
            + "- Invalid base64 encoding in GaenRequest"
            + "- negative rolling period"
            + "- fake claim with non-fake keys",
        "403=>Authentication failed"
      })
  public @ResponseBody Callable<ResponseEntity<String>> addExposed(
      @Documentation(
              description =
                  "JSON Object containing all keys and a list of countries specifying for which"
                      + " countries the keys are valid or more precise what countries have been"
                      + " visited")
          @Valid
          @RequestBody
          GaenV2UploadKeysRequest gaenV2Request,
      @RequestHeader(value = "User-Agent")
          @Documentation(
              description =
                  "App Identifier (PackageName/BundleIdentifier) + App-Version + OS (Android/iOS)"
                      + " + OS-Version",
              example = "ch.ubique.android.starsdk;1.0;iOS;13.3")
          String userAgent,
      @AuthenticationPrincipal
          @Documentation(description = "JWT token that can be verified by the backend server")
          Object principal)
      throws WrongScopeException, InsertException {
    var now = UTCInstant.now();

    this.validateRequest.isValid(principal);

    // Filter out non valid keys and insert them into the database (c.f.
    // InsertManager and
    // configured Filters in the WSBaseConfig)
    insertManager.insertIntoDatabase(
        gaenV2Request.getGaenKeys(),
        gaenV2Request.getCountriesForSharingKeys(),
        userAgent,
        principal,
        now);
    var responseBuilder = ResponseEntity.ok();
    Callable<ResponseEntity<String>> cb =
        () -> {
          try {
            now.normalizeDuration(requestTime);
          } catch (DurationExpiredException e) {
            logger.error("Total time spent in endpoint is longer than requestTime");
          }
          return responseBuilder.body("OK");
        };
    return cb;
  }

  // GET for Key Download
  @GetMapping(value = "/exposed")
  @Documentation(
      description =
          "Requests the exposed keys published _since_ originating from list of _country_",
      responses = {
        "200 => zipped export.bin and export.sig of all keys in that interval",
        "404 => Invalid _country_ or invalid _since_ (too far in the past/future, not at bucket"
            + " boundaries)"
      })
  public @ResponseBody ResponseEntity<byte[]> getExposedKeys(
      @Documentation(
              description =
                  "List of origin countries of requested keys. (iso-3166-1 alpha-2). Must be"
                      + " padded with fake countries",
              example = "CH")
          @RequestParam
          List<String> country,
      @Documentation(
              description =
                  "Timestamp to retrieve exposed keys since, in milliseconds since Unix epoch"
                      + " (1970-01-01). It must indicate the beginning of a bucket.",
              example = "1593043200000")
          @RequestParam
          long since)
      throws BadBatchReleaseTimeException, InvalidKeyException, SignatureException,
          NoSuchAlgorithmException, IOException {
    var now = UTCInstant.now();
    var keysSince = UTCInstant.ofEpochMillis(since);
    if (!validationUtils.isValidBatchReleaseTime(keysSince, now)) {
      return ResponseEntity.notFound().build();
    }
    UTCInstant publishedUntil = now.roundToBucketStart(releaseBucketDuration);
    // TODO: get keys based on countries
    var exposedKeys = new ArrayList<GaenKey>();

    // TODO We need padding

    if (exposedKeys.isEmpty()) {
      return ResponseEntity.noContent()
          .cacheControl(CacheControl.maxAge(exposedListCacheControl))
          .header("X-PUBLISHED-UNTIL", Long.toString(publishedUntil.getTimestamp()))
          .build();
    }
    ProtoSignatureWrapper payload = gaenSigner.getPayload(exposedKeys);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(exposedListCacheControl))
        .header("X-PUBLISHED-UNTIL", Long.toString(publishedUntil.getTimestamp()))
        .body(payload.getZip());
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
