package org.dpppt.backend.sdk.interops;

import org.dpppt.backend.sdk.interops.syncer.IrishHubSyncer;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

class SyncTest {

  @Test
  @Ignore("for local testing")
  void test() {
    IrishHubSyncer syncer =
        new IrishHubSyncer("https://interop-qa.nf-covid-services.com", "", 14, null);
    syncer.sync();
    syncer.sync();
  }
}
