package org.dpppt.backend.sdk.ws.controller;

import ch.ubique.openapi.docannotations.Documentation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;
import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** This is a controller to help with load-testing (allows to generated random keys in the db) */
@Controller
@RequestMapping("/loadtest")
@Documentation(description = "The load-test endpoint to generate keys")
public class LoadTestController {
  private static final Logger logger = LoggerFactory.getLogger(LoadTestController.class);

  @Autowired private final GaenDataService dataService;

  private final int gaenKeySizeBytes;

  private static final Random random = new Random();

  public LoadTestController(GaenDataService dataservice, int gaenKeySizeBytes) {
    this.dataService = dataservice;
    this.gaenKeySizeBytes = gaenKeySizeBytes;
  }

  // GET to trigger key generation
  @GetMapping(value = "/generate-keys")
  @Documentation(
      description = "Generate random keys and insert into database for load-testing purposes.",
      responses = {
        "200 => All good",
        "404 => Invalid parameters (numberOfKeys, withFederationGateway,...)"
      })
  public @ResponseBody ResponseEntity<ArrayList<GaenKey>> generateKeys(
      @Documentation(description = "Number of keys to insert into db") @RequestParam
          Long numberOfKeys,
      @RequestParam(required = false, defaultValue = "false") Boolean withFederationGateway) {
    var now = UTCInstant.now();

    var keys = new ArrayList<GaenKey>();
    for (int i = 0; i < numberOfKeys; i++) {
      byte[] keyData = new byte[gaenKeySizeBytes];
      random.nextBytes(keyData);
      var keyGaenTime = (int) now.atStartOfDay().minus(Duration.ofDays(1)).get10MinutesSince1970();
      var key = new GaenKey(Base64.getEncoder().encodeToString(keyData), keyGaenTime, 144);
      keys.add(key);
    }

    this.dataService.upsertExposees(keys, now, withFederationGateway);

    return ResponseEntity.ok().body(keys);
  }
}
