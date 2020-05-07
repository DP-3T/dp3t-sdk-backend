package org.dpppt.backend.sdk.ws.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1/gaen")
public class GaenController {
    
    @PostMapping(value = "/exposed")
    public @ResponseBody ResponseEntity<String> addExposed(@Valid @RequestBody GaenRequest gaenRequest,
                                                           @RequestHeader(value = "User-Agent", required = true) String userAgent,
                                                           @AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok("OK");
    }
}