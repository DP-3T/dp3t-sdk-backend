package org.dpppt.backend.sdk.ws.config;

import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.ws.controller.LoadTestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("load-test")
public class WSLoadConfig {
  @Value("${ws.app.gaen.key_size: 16}")
  int gaenKeySizeBytes;

  @Bean
  LoadTestController loadTestController(GaenDataService gaenDataService) {
    return new LoadTestController(gaenDataService, gaenKeySizeBytes);
  }
}
