package org.dpppt.backend.sdk.interops;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.dpppt.backend.sdk.data.gaen.GaenDataService;
import org.dpppt.backend.sdk.data.interops.SyncLogDataService;
import org.dpppt.backend.sdk.interops.model.EfgsGatewayConfig;
import org.dpppt.backend.sdk.interops.syncer.EfgsHubSyncer;
import org.dpppt.backend.sdk.interops.syncer.IrishHubSyncer;
import org.dpppt.backend.sdk.interops.syncer.efgs.EfgsClient;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenKeyWithOrigin;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"dev"})
@TestPropertySource(properties = {"ws.origin.country=CH"})
class SyncTest {

  @Autowired private GaenDataService gaenDataService;

  @Autowired private SyncLogDataService syncLogDataService;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
  void testEfgsClientUpload()
      throws GeneralSecurityException, OperatorCreationException, CMSException, IOException {
    EfgsClient efgsClient = new EfgsClient(getEfgsGatewayConfig());
    String batchTag = getBatchTag();
    List<GaenKeyWithOrigin> keysToUpload = createMockedKeys(10);
    List<GaenKeyWithOrigin> uploadedKeys = efgsClient.upload(keysToUpload, batchTag);
    Assert.assertEquals(keysToUpload.size(), uploadedKeys.size());
  }

  @Test
  @Ignore("for local testing")
  void testEfgsClientDownload() throws GeneralSecurityException {
    EfgsHubSyncer syncer =
        new EfgsHubSyncer(
            new EfgsClient(getEfgsGatewayConfig()),
            Duration.ofDays(14),
            gaenDataService,
            syncLogDataService);
    syncer.download(UTCInstant.today().getLocalDate());
  }

  private EfgsGatewayConfig getEfgsGatewayConfig() {
    EfgsGatewayConfig efgsGatewayConfig = new EfgsGatewayConfig();
    efgsGatewayConfig.setId("efgs-gateway");
    efgsGatewayConfig.setBaseUrl("https://api.ch-hub-r.bit.admin.ch");
    efgsGatewayConfig.setAuthClientCert("base64:/*");
    efgsGatewayConfig.setAuthClientCertPassword("*");
    efgsGatewayConfig.setSignClientCert(
        "-----BEGIN CERTIFICATE-----\n" + "*\n" + "-----END CERTIFICATE-----\n");
    efgsGatewayConfig.setSignClientCertPrivateKey(
        "-----BEGIN PRIVATE KEY-----\n" + "*\n" + "-----END PRIVATE KEY-----\n");
    efgsGatewayConfig.setSignAlgorithmName("sha256WithRSAEncryption");
    efgsGatewayConfig.setVisitedCountries(List.of("CH", "DE"));
    return efgsGatewayConfig;
  }

  private List<GaenKeyWithOrigin> createMockedKeys(int numOfKeysToCreate) {
    List<GaenKeyWithOrigin> keys = new ArrayList<>();
    for (int i = 0; i < numOfKeysToCreate; i++) {
      byte[] bytes = new byte[16];
      SECURE_RANDOM.nextBytes(bytes);
      GaenKeyWithOrigin keyWithOrigin = new GaenKeyWithOrigin();
      keyWithOrigin.setGaenKey(new GaenKey());
      keyWithOrigin.setKeyData(java.util.Base64.getEncoder().encodeToString(bytes));
      keyWithOrigin.setRollingStartNumber(
          (int) UTCInstant.now().atStartOfDay().minusDays(1).get10MinutesSince1970());
      keyWithOrigin.setRollingPeriod(144);
      keyWithOrigin.setTransmissionRiskLevel(0);
      keyWithOrigin.setFake(0);
      keyWithOrigin.setOrigin("CH");
      keyWithOrigin.setId("test-" + i);
      keys.add(keyWithOrigin);
    }
    return keys;
  }

  private String getBatchTag() {
    byte[] hash = new byte[4];
    SECURE_RANDOM.nextBytes(hash);
    var now = LocalDateTime.now(ZoneOffset.UTC);
    return String.format(
        "%d-%d-%d-%s-%d",
        now.getYear(),
        now.getMonth().getValue(),
        now.getDayOfMonth(),
        Base64.encodeBase64String(hash),
        0);
  }
}
