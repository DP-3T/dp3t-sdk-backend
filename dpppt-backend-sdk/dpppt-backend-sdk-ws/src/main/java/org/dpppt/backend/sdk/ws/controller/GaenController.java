package org.dpppt.backend.sdk.ws.controller;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.google.protobuf.ByteString;

import org.dpppt.backend.sdk.data.EtagGeneratorInterface;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.DayBuckets;
import org.dpppt.backend.sdk.model.gaen.File;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.model.gaen.GaenSecondDay;
import org.dpppt.backend.sdk.model.gaen.Header;
import org.dpppt.backend.sdk.model.gaen.proto.FileProto;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.security.ValidateRequest.InvalidDateException;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.dpppt.backend.sdk.ws.util.GaenUnit;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.dpppt.backend.sdk.ws.util.ValidationUtils.BadBatchReleaseTimeException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
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

import io.jsonwebtoken.Jwts;

@Controller
@RequestMapping("/v1/gaen")
public class GaenController {

    private final Duration bucketLength;
    private final Duration requestTime;
    private final ValidateRequest validateRequest;
    private final ValidationUtils validationUtils;
    private final EtagGeneratorInterface etagGenerator;
    private final GAENDataService dataService;
    private final Duration exposedListCacheContol;
    private final PrivateKey secondDayKey;
    private final ProtoSignature gaenSigner;
    private final String gaenRegion;

    public GaenController(GAENDataService dataService, EtagGeneratorInterface etagGenerator,
            ValidateRequest validateRequest, ProtoSignature gaenSigner, ValidationUtils validationUtils,
            Duration bucketLength, Duration requestTime, Duration exposedListCacheContol,
            PrivateKey secondDayKey, String gaenRegion) {
        this.dataService = dataService;
        this.bucketLength = bucketLength;
        this.validateRequest = validateRequest;
        this.requestTime = requestTime;
        this.validationUtils = validationUtils;
        this.etagGenerator = etagGenerator;
        this.exposedListCacheContol = exposedListCacheContol;
        this.secondDayKey = secondDayKey;
        this.gaenSigner = gaenSigner;
        this.gaenRegion = gaenRegion;
    }

    @PostMapping(value = "/exposed")
    public @ResponseBody ResponseEntity<String> addExposed(@Valid @RequestBody GaenRequest gaenRequest,
            @RequestHeader(value = "User-Agent", required = true) String userAgent,
            @AuthenticationPrincipal Object principal) throws InvalidDateException {
        var now = Instant.now().toEpochMilli();
        if (!this.validateRequest.isValid(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<GaenKey> nonFakeKeys = new ArrayList<>();
        for (var key : gaenRequest.getGaenKeys()) {
            if (!validationUtils.isValidBase64Key(key.getKeyData())) {
                return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
            }
            this.validateRequest.getKeyDate(principal, key);
            if (this.validateRequest.isFakeRequest(principal, key)) {
                continue;
            } else {
                nonFakeKeys.add(key);
            }
        }
        if (principal instanceof Jwt && ((Jwt) principal).containsClaim("fake")
                && ((Jwt) principal).getClaim("fake").equals("1") && !nonFakeKeys.isEmpty()) {
            return ResponseEntity.badRequest().body("Claim is fake but list contains non fake keys");
        }
        if (!nonFakeKeys.isEmpty()) {
            dataService.upsertExposees(nonFakeKeys);
        }

        var delayedKeyDateDuration = Duration.of(gaenRequest.getDelayedKeyDate(), GaenUnit.TenMinutes);
        var delayedKeyDate = LocalDate.ofInstant(Instant.ofEpochMilli(delayedKeyDateDuration.toMillis()), ZoneOffset.UTC);
      
        var nowDay = LocalDate.now(ZoneOffset.UTC);
        if(!delayedKeyDate.isAfter(nowDay.minusDays(1)) && delayedKeyDate.isBefore(nowDay.plusDays(1))) {
            return ResponseEntity.badRequest().body("delayedKeyDate date must be between yesterday and tomorrow");
        }
       
        var responseBuilder = ResponseEntity.ok();
        if (principal instanceof Jwt) {
            var originalJWT = (Jwt) principal;
            var jwtBuilder = Jwts.builder().setId(UUID.randomUUID().toString()).setIssuedAt(Date.from(Instant.now()))
                    .setIssuer("dpppt-sdk-backend")
                    .setSubject(originalJWT.getSubject())
                    .setExpiration(Date.from(delayedKeyDate.atStartOfDay().toInstant(ZoneOffset.UTC).plus(Duration.ofHours(48))))
                    .claim("scope", "currentDayExposed")
                    .claim("delayedKeyDate", gaenRequest.getDelayedKeyDate());
            if (originalJWT.containsClaim("fake")) {
                jwtBuilder.claim("fake", originalJWT.getClaim("fake"));
            }
            String jwt = jwtBuilder.signWith(secondDayKey).compact();
            responseBuilder.header("Authorization", "Bearer " + jwt);
        }

        normalizeRequestTime(now);
        return responseBuilder.build();
    }

    @PostMapping(value = "/exposednextday")
    public @ResponseBody ResponseEntity<String> addExposedSecond(@Valid @RequestBody GaenSecondDay gaenSecondDay,
            @RequestHeader(value = "User-Agent", required = true) String userAgent,
            @AuthenticationPrincipal Object principal) throws InvalidDateException {
        var now = Instant.now().toEpochMilli();
       
        if (!validationUtils.isValidBase64Key(gaenSecondDay.getDelayedKey().getKeyData())) {
            return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
        }
        if(principal instanceof Jwt && !((Jwt)principal).containsClaim("delayedKeyDate")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("claim does not contain delayedKeyDate");
        }
        if(principal instanceof Jwt) {
            var jwt = (Jwt)principal;
            var claimKeyDate = Integer.parseInt(jwt.getClaimAsString("delayedKeyDate"));
            if(!gaenSecondDay.getDelayedKey().getRollingStartNumber().equals(Integer.valueOf(claimKeyDate))) {
                return ResponseEntity.badRequest().body("keyDate does not match claim keyDate");
            }
        }
        if (!this.validateRequest.isFakeRequest(principal, gaenSecondDay.getDelayedKey())) {
            List<GaenKey> keys = new ArrayList<>();
            keys.add(gaenSecondDay.getDelayedKey());
            dataService.upsertExposees(keys);
        }
        normalizeRequestTime(now);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/exposedios/{batchReleaseTime}", produces = "application/x-protobuf")
    public @ResponseBody ResponseEntity<FileProto.File> getExposedKeysIos(@PathVariable Long batchReleaseTime,
            WebRequest request) throws BadBatchReleaseTimeException {
        
        var batchReleaseTimeDuration = Duration.ofMillis(batchReleaseTime);

        if(!validationUtils.isValidBatchReleaseTime(batchReleaseTimeDuration.toMillis())) {
            return ResponseEntity.notFound().build();
        }

        int max = dataService.getMaxExposedIdForBatchReleaseTime(batchReleaseTimeDuration.toMillis(), bucketLength.toMillis());
        String etag = etagGenerator.getEtag(max, "proto");
        
        if (request.checkNotModified(etag)) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
       var file = FileProto.File.newBuilder();
        var exposedKeys = dataService.getSortedExposedForBatchReleaseTime(batchReleaseTimeDuration.toMillis(), bucketLength.toMillis());
        for(var key : exposedKeys) {
            var protoKey = FileProto.Key.newBuilder()
                .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode(key.getKeyData())))
                .setRollingPeriod(key.getRollingPeriod())
                .setRollingStartNumber(key.getRollingStartNumber())
                .setTransmissionRiskLevel(key.getTransmissionRiskLevel()).build();
            file.addKey(protoKey);
        }
        
        var header = FileProto.Header.newBuilder();
        header.setRegion(gaenRegion)
            .setStartTimestamp(batchReleaseTimeDuration.toSeconds())
            .setEndTimestamp(batchReleaseTimeDuration.toSeconds() + bucketLength.toSeconds());
        file.setHeader(header);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(exposedListCacheContol))
        .header("X-BATCH-RELEASE-TIME", Long.toString(batchReleaseTimeDuration.toMillis())).body(file.build());
    }

    @GetMapping(value = "/exposed/{batchReleaseTime}", produces = "application/zip")
    public @ResponseBody ResponseEntity<byte[]> getExposedKeys(@PathVariable Long batchReleaseTime, WebRequest request)
            throws BadBatchReleaseTimeException, IOException, InvalidKeyException, SignatureException,
            NoSuchAlgorithmException {

        var batchReleaseTimeDuration = Duration.ofMillis(batchReleaseTime);

        if (!validationUtils.isValidBatchReleaseTime(batchReleaseTimeDuration.toMillis())) {
            return ResponseEntity.notFound().build();
        }

        int max = dataService.getMaxExposedIdForBatchReleaseTime(batchReleaseTimeDuration.toMillis(),
                bucketLength.toMillis());
        String etag = etagGenerator.getEtag(max, "proto");

        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        var exposedKeys = dataService.getSortedExposedForBatchReleaseTime(batchReleaseTimeDuration.toMillis(), bucketLength.toMillis());
        var keysGroupedByRollingStartNumber = exposedKeys.stream().collect(Collectors.groupingBy(GaenKey::getRollingStartNumber)).values();
        byte[] payload = gaenSigner.getPayload(keysGroupedByRollingStartNumber);
        
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(exposedListCacheContol))
                .header("X-BATCH-RELEASE-TIME", Long.toString(batchReleaseTimeDuration.toMillis()))
                .body(payload);
    }

    @GetMapping(value = "/exposedjson/{batchReleaseTime}", produces = "application/json")
    public @ResponseBody ResponseEntity<File> getExposedKeysAsJson(@PathVariable Long batchReleaseTime,
            WebRequest request) throws BadBatchReleaseTimeException {
        var batchReleaseTimeDuration = Duration.ofMillis(batchReleaseTime);

        if (!validationUtils.isValidBatchReleaseTime(batchReleaseTimeDuration.toMillis())) {
            return ResponseEntity.notFound().build();
        }
        int max = dataService.getMaxExposedIdForBatchReleaseTime(batchReleaseTimeDuration.toMillis(),
                bucketLength.toMillis());
        String etag = etagGenerator.getEtag(max, "json");
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        var exposedKeys = dataService.getSortedExposedForBatchReleaseTime(batchReleaseTimeDuration.toMillis(),
                bucketLength.toMillis());
        var file = new File();
        var header = new Header();
        header.startTimestamp(batchReleaseTimeDuration.toSeconds())
                .endTimestamp(batchReleaseTimeDuration.toSeconds() + bucketLength.toSeconds());
        file.gaenKeys(exposedKeys).header(header);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(exposedListCacheContol))
                .header("X-BATCH-RELEASE-TIME", Long.toString(batchReleaseTimeDuration.toMillis())).body(file);
    }

    @GetMapping(value = "/buckets/{dayDateStr}")
    public @ResponseBody ResponseEntity<DayBuckets> getBuckets(@PathVariable String dayDateStr) {
        var atStartOfDay = LocalDate.parse(dayDateStr).atStartOfDay().toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC);
        var end = atStartOfDay.plusDays(1);
        var now = Instant.now().atOffset(ZoneOffset.UTC);
        if (!validationUtils.isDateInRange(atStartOfDay)) {
            return ResponseEntity.notFound().build();
        }
        var relativeUrls = new ArrayList<String>();
        var dayBuckets = new DayBuckets();

        String controllerMapping = this.getClass().getAnnotation(RequestMapping.class).value()[0];
        dayBuckets.day(dayDateStr).relativeUrls(relativeUrls);

        while (atStartOfDay.toInstant().toEpochMilli() < Math.min(now.toInstant().toEpochMilli(),
                end.toInstant().toEpochMilli())) {
            relativeUrls.add(controllerMapping + "/exposed" + "/" + atStartOfDay.toInstant().toEpochMilli());
            atStartOfDay = atStartOfDay.plus(this.bucketLength);
        }

        return ResponseEntity.ok(dayBuckets);
    }

    private void normalizeRequestTime(long now) {
        long after = Instant.now().toEpochMilli();
        long duration = after - now;
        try {
            Thread.sleep(Math.max(requestTime.minusMillis(duration).toMillis(), 0));
        } catch (Exception ex) {

        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> invalidArguments() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(InvalidDateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> invalidDate() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(BadBatchReleaseTimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> invalidBatchReleaseTime() {
        return ResponseEntity.badRequest().build();
    }
}