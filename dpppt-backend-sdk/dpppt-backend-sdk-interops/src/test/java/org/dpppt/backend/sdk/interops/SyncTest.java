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
  public void testEfgsClientDownload() throws GeneralSecurityException {
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
    efgsGatewayConfig.setSignClientCert(
        "-----BEGIN CERTIFICATE-----\n*\n-----END CERTIFICATE-----\n");
    efgsGatewayConfig.setSignClientCertPrivateKey(
        "-----BEGIN PRIVATE KEY-----\n*\n-----END PRIVATE KEY-----\n");
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
