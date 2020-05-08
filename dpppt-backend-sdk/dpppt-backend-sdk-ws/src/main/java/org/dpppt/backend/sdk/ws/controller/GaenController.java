package org.dpppt.backend.sdk.ws.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.dpppt.backend.sdk.model.gaen.DayBuckets;
import org.dpppt.backend.sdk.model.gaen.File;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.model.gaen.proto.FileProto;
import org.dpppt.backend.sdk.ws.security.ValidateRequest;
import org.dpppt.backend.sdk.ws.util.ValidationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/v1/gaen")
public class GaenController {

    private final Integer retentionPeriod;
    private final Duration bucketLength;
    private final Duration requestTime;
    private final ValidateRequest validateRequest;
    private final ValidationUtils validationUtils;

    public GaenController(ValidateRequest validateRequest, ValidationUtils validationUtils, Integer retentionPeriod, Duration bucketLength, Duration requestTime) {
        this.retentionPeriod = retentionPeriod;
        this.bucketLength = bucketLength;
        this.validateRequest = validateRequest;
        this.requestTime = requestTime;
        this.validationUtils = validationUtils;
    }

    @PostMapping(value = "/exposed")
    public @ResponseBody ResponseEntity<String> addExposed(@Valid @RequestBody GaenRequest gaenRequest,
            @RequestHeader(value = "User-Agent", required = true) String userAgent,
            @AuthenticationPrincipal Object principal) {
        var now = Instant.now().toEpochMilli();
        if(!this.validateRequest.isValid(principal)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        for(var key : gaenRequest.getGaenKeys()) {
            if(!validationUtils.isValidBase64Key(key.getKeyData())) {
                return new ResponseEntity<>("No valid base64 key", HttpStatus.BAD_REQUEST);
            }
            this.validateRequest.getKeyDate(principal, key);
        }
        if(!this.validateRequest.isFakeRequest(principal, gaenRequest)) {
            dataService.upsertExposees(gaenRequest.getGaenKeys());
        }
        long after = Instant.now().toEpochMilli();
        long duration = after - now;
        try {
            Thread.sleep(Math.max(this.requestTime.toMillis() - duration, 0));
        }
        catch (Exception ex) {

        }
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/exposed/{batchReleaseTime}", produces = "application/x-protobuf")
    public @ResponseBody ResponseEntity<FileProto.File> getExposedKeys(@PathVariable Long batchReleaseTime,
            WebRequest request) {
        var file = FileProto.File.getDefaultInstance();

        return ResponseEntity.ok(file);
    }

    @GetMapping(value = "/exposedjson/{batchReleaseTime}", produces = "application/json")
    public @ResponseBody ResponseEntity<File> getExposedKeysAsJson(@PathVariable Long batchReleaseTime,
            WebRequest request) {
        var file = new File();

        return ResponseEntity.ok(file);
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
}