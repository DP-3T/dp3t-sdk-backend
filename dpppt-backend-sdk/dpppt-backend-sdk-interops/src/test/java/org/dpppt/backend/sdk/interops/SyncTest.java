package org.dpppt.backend.sdk.interops;

import java.time.Duration;
import org.dpppt.backend.sdk.interops.syncer.EFGSHubSyncer;
import org.dpppt.backend.sdk.interops.syncer.IrishHubSyncer;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

class SyncTest {

  @Test
  @Ignore("for local testing")
  void test() {
    IrishHubSyncer syncer =
        new IrishHubSyncer(
            "https://interop-qa.nf-covid-services.com",
            "",
            "",
            Duration.ofDays(10),
            Duration.ofHours(2),
            null,
            "CH");
    syncer.sync();
  }

  @Test
  @Ignore("for local testing")
  void testEfgs() {
    EFGSHubSyncer syncer = null;
  }
}
