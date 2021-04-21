/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

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
import org.dpppt.backend.sdk.interops.config.FlyWayConfig;
import org.dpppt.backend.sdk.interops.config.GaenDataServiceConfig;
import org.dpppt.backend.sdk.interops.config.InteropsInsertManagerConfig;
import org.dpppt.backend.sdk.interops.config.StandaloneDataConfig;
import org.dpppt.backend.sdk.interops.config.SyncLogDataServiceConfig;
import org.dpppt.backend.sdk.interops.insertmanager.InteropsInsertManager;
import org.dpppt.backend.sdk.interops.model.EfgsGatewayConfig;
import org.dpppt.backend.sdk.interops.syncer.EfgsHubSyncer;
import org.dpppt.backend.sdk.interops.syncer.efgs.EfgsClient;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    loader = AnnotationConfigContextLoader.class,
    classes = {
      StandaloneDataConfig.class,
      FlyWayConfig.class,
      GaenDataServiceConfig.class,
      SyncLogDataServiceConfig.class,
      InteropsInsertManagerConfig.class
    })
public class SyncTest {

  @Autowired private GaenDataService gaenDataService;

  @Autowired private SyncLogDataService syncLogDataService;

  @Autowired private InteropsInsertManager interopsInsertManager;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private static String testP12 = "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";
  private static String testPassword = "testPassword";

  @Test
  @Ignore("for local testing")
  public void testEfgsClientUpload()
      throws GeneralSecurityException, OperatorCreationException, CMSException, IOException {
    EfgsClient efgsClient = new EfgsClient(getEfgsGatewayConfig());
    String batchTag = getBatchTag();
    List<GaenKeyForInterops> keysToUpload = createMockedKeys(10);
    List<GaenKeyForInterops> uploadedKeys = efgsClient.upload(keysToUpload, batchTag);
    Assert.assertEquals(keysToUpload.size(), uploadedKeys.size());
  }

  @Test
  @Ignore("for local testing")
  public void testEfgsClientDownload() throws GeneralSecurityException, IOException {
    EfgsHubSyncer syncer =
        new EfgsHubSyncer(
            new EfgsClient(getEfgsGatewayConfig()),
            Duration.ofDays(14),
            gaenDataService,
            syncLogDataService,
            interopsInsertManager);
    syncer.download(UTCInstant.today().getLocalDate());
  }

  private EfgsGatewayConfig getEfgsGatewayConfig() {
    EfgsGatewayConfig efgsGatewayConfig = new EfgsGatewayConfig();
    efgsGatewayConfig.setId("efgs-gateway");
    efgsGatewayConfig.setBaseUrl("https://api-ch-hub-r.bag.admin.ch");
    efgsGatewayConfig.setAuthClientCert("base64:/*");
    efgsGatewayConfig.setAuthClientCertPassword("*");
    efgsGatewayConfig.setSignClientCert(testP12);
    efgsGatewayConfig.setSignClientCertPassword(testPassword);
    efgsGatewayConfig.setSignAlgorithmName("sha256WithRSAEncryption");
    efgsGatewayConfig.setVisitedCountries(List.of("CH", "DE"));
    return efgsGatewayConfig;
  }

  private List<GaenKeyForInterops> createMockedKeys(int numOfKeysToCreate) {
    List<GaenKeyForInterops> keys = new ArrayList<>();
    for (int i = 0; i < numOfKeysToCreate; i++) {
      byte[] bytes = new byte[16];
      SECURE_RANDOM.nextBytes(bytes);
      GaenKeyForInterops keyWithOrigin = new GaenKeyForInterops();
      keyWithOrigin.setGaenKey(new GaenKey());
      keyWithOrigin.setKeyData(java.util.Base64.getEncoder().encodeToString(bytes));
      keyWithOrigin.setRollingStartNumber(
          (int) UTCInstant.now().atStartOfDay().minusDays(1).get10MinutesSince1970());
      keyWithOrigin.setRollingPeriod(144);
      keyWithOrigin.setTransmissionRiskLevel(0);
      keyWithOrigin.setFake(0);
      keyWithOrigin.setOrigin("CH");
      keyWithOrigin.setId(i);
      keyWithOrigin.setReceivedAt(UTCInstant.now());
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
