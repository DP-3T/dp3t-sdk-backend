package org.dpppt.backend.sdk.ws.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import javax.validation.Valid;

import org.dpppt.backend.sdk.model.gaen.DayBuckets;
import org.dpppt.backend.sdk.model.gaen.File;
import org.dpppt.backend.sdk.model.gaen.GaenRequest;
import org.dpppt.backend.sdk.model.gaen.proto.FileProto;
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

    public GaenController(Integer retentionPeriod, Duration bucketLength) {
        this.retentionPeriod = retentionPeriod;
        this.bucketLength = bucketLength;
    }

    @PostMapping(value = "/exposed")
    public @ResponseBody ResponseEntity<String> addExposed(@Valid @RequestBody GaenRequest gaenRequest,
            @RequestHeader(value = "User-Agent", required = true) String userAgent,
            @AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok("OK");
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
        if (!isInRange(timestamp)) {
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

    private boolean isInRange(OffsetDateTime timestamp) {
        if (timestamp.isAfter(Instant.now().atOffset(ZoneOffset.UTC))) {
            return false;
        }
        if (timestamp.isBefore(Instant.now().atOffset(ZoneOffset.UTC).minusDays(retentionPeriod))) {
            return false;
        }
        return true;
    }
}