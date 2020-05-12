package org.dpppt.backend.sdk.ws.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.validation.Valid;

import com.google.protobuf.ByteString;

import org.dpppt.backend.sdk.data.EtagGeneratorInterface;
import org.dpppt.backend.sdk.data.gaen.GAENDataService;
import org.dpppt.backend.sdk.model.gaen.DayBuckets;
import org.dpppt.backend.sdk.model.gaen.File;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.model.gaen.Header;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat.SignatureInfo;
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

    private final Integer retentionPeriod;
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
            Integer retentionPeriod, Duration bucketLength, Duration requestTime, Duration exposedListCacheContol,
            PrivateKey secondDayKey, String gaenRegion) {
        this.dataService = dataService;
        this.retentionPeriod = retentionPeriod;
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
            }
            nonFakeKeys.add(key);
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
                    .setExpiration(Date.from(delayedKeyDate.atStartOfDay().toInstant(ZoneOffset.UTC).plus(Duration.ofHours(48))))
                    .claim("scope", "currentDayExposed")
                    .claim("expectedKeyDate", gaenRequest.getDelayedKeyDate());
            if (originalJWT.containsClaim("fake")) {
                jwtBuilder.claim("fake", originalJWT.getClaim("fake"));
            }
            String jwt = jwtBuilder.signWith(secondDayKey).compact();
            responseBuilder.header("Authentication", "Bearer " + jwt);
        }

        long after = Instant.now().toEpochMilli();
        long duration = after - now;
        try {
            Thread.sleep(Math.max(requestTime.minusMillis(duration).toMillis(), 0));
        } catch (Exception ex) {

        }
        return responseBuilder.build();
    }

    @PostMapping(value = "/exposednextday")
    public @ResponseBody ResponseEntity<String> addExposedSecond(@Valid @RequestBody GaenRequest gaenRequest,
            @RequestHeader(value = "User-Agent", required = true) String userAgent,
            @AuthenticationPrincipal Object principal) throws InvalidDateException {
        var now = Instant.now().toEpochMilli();

        // if(!this.validateRequest.isValid(principal)){
        // return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        // }
        for (var key : gaenRequest.getGaenKeys()) {
            if (!validationUtils.isValidBase64Key(key.getKeyData())) {
                return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
            }
            this.validateRequest.getKeyDate(principal, key);
        }
        if (!this.validateRequest.isFakeRequest(principal, gaenRequest)) {
            dataService.upsertExposees(gaenRequest.getGaenKeys());
        }
        long after = Instant.now().toEpochMilli();
        long duration = after - now;
        try {
            Thread.sleep(Math.max(requestTime.minusMillis(duration).toMillis(), 0));
        } catch (Exception ex) {

        }
        return ResponseEntity.ok().build();
    }

    private TemporaryExposureKeyFormat.TemporaryExposureKeyExport getProtoKey(Duration batchReleaseTimeDuration,
            SignatureInfo tekSignature) {
        var file = TemporaryExposureKeyFormat.TemporaryExposureKeyExport.newBuilder();
        var exposedKeys = dataService.getSortedExposedForBatchReleaseTime(batchReleaseTimeDuration.toMillis(),
                bucketLength.toMillis());
        var tekList = new ArrayList<TemporaryExposureKeyFormat.TemporaryExposureKey>();
        for (var key : exposedKeys) {
            var protoKey = TemporaryExposureKeyFormat.TemporaryExposureKey.newBuilder()
                    .setKeyData(ByteString.copyFrom(Base64.getDecoder().decode(key.getKeyData())))
                    .setRollingPeriod(key.getRollingPeriod()).setRollingStartIntervalNumber(key.getRollingStartNumber())
                    .setTransmissionRiskLevel(key.getTransmissionRiskLevel()).build();
            tekList.add(protoKey);
        }

        file.addAllKeys(tekList);

        file.setRegion(gaenRegion).setBatchNum(1).setBatchSize(1)
                .setStartTimestamp(batchReleaseTimeDuration.toSeconds())
                .setEndTimestamp(batchReleaseTimeDuration.toSeconds() + bucketLength.toSeconds());

        file.addSignatureInfos(tekSignature);

        return file.build();
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

        var tekSignature = gaenSigner.getSignatureInfo();
        var file = getProtoKey(batchReleaseTimeDuration, tekSignature);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(byteOut);

        zip.putNextEntry(new ZipEntry("export.bin"));
        byte[] exportBin = file.toByteArray();
        zip.write("EK Export v1    ".getBytes());
        zip.write(exportBin);
        zip.closeEntry();

        var signatureList = gaenSigner.getSignatureObject(exportBin, tekSignature);

        byte[] exportSig = signatureList.toByteArray();
        zip.putNextEntry(new ZipEntry("export.sig"));
        zip.write(exportSig);
        zip.closeEntry();

        zip.flush();
        zip.close();
        byteOut.close();

        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(exposedListCacheContol))
                .header("X-BATCH-RELEASE-TIME", Long.toString(batchReleaseTimeDuration.toMillis()))
                .body(byteOut.toByteArray());
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
        var timestamp = LocalDate.parse(dayDateStr).atStartOfDay().toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC);
        var now = Instant.now().atOffset(ZoneOffset.UTC);
        if (!validationUtils.isDateInRange(timestamp)) {
            return ResponseEntity.notFound().build();
        }
        var relativeUrls = new ArrayList<String>();
        var dayBuckets = new DayBuckets();

        String controllerMapping = this.getClass().getAnnotation(RequestMapping.class).value()[0];
        dayBuckets.day(dayDateStr).relativeUrls(relativeUrls);

        while (timestamp.toInstant().toEpochMilli() < Math.min(now.toInstant().toEpochMilli(),
                timestamp.plusDays(1).toInstant().toEpochMilli())) {
            relativeUrls.add(controllerMapping + "/exposed" + "/" + timestamp.toInstant().toEpochMilli());
            timestamp = timestamp.plus(this.bucketLength);
        }

        return ResponseEntity.ok(dayBuckets);
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