package org.dpppt.backend.sdk.ws.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("/v1/gaen")
public class GaenController {

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
        var dayBuckets = new DayBuckets();
        return ResponseEntity.ok(dayBuckets);
    }
}