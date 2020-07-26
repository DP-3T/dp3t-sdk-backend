package org.dpppt.backend.sdk.ws.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.proto.TemporaryExposureKeyFormat;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.dpppt.backend.sdk.ws.security.signature.ProtoSignature;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({ "actuator-security" })
@SpringBootTest(properties = { "ws.app.jwt.publickey=classpath://generated_pub.pem",
		"logging.level.org.springframework.security=DEBUG", "ws.exposedlist.releaseBucketDuration=7200000", "ws.gaen.randomkeysenabled=true",
		"ws.app.gaen.delayTodaysKeys=true",
	"ws.monitor.prometheus.user=prometheus",
	"ws.monitor.prometheus.password=prometheus",
	"management.endpoints.enabled-by-default=true",
	"management.endpoints.web.exposure.include=*"})
@Transactional
@Execution(ExecutionMode.SAME_THREAD)
public class GaenControllerTestNotThreadSafe extends BaseControllerTest {
    @Autowired
    ProtoSignature signer;
    
    private static final Logger logger = LoggerFactory.getLogger(GaenControllerTest.class);

    @Test
    @Transactional
	public void zipContainsFiles() throws Exception {
		var clockStartingAtMidnight = Clock.offset(Clock.systemUTC(), UTCInstant.now().getDuration(UTCInstant.today()).negated());
		UTCInstant.setClock(clockStartingAtMidnight);
		var now = UTCInstant.now();
		var midnight = now.atStartOfDay();

		// insert two times 5 keys per day for the last 14 days. the second batch has a
		// different received at timestamp. (+6 hours)
		insertNKeysPerDayInInterval(14,
				midnight.minusDays(4),
				now, now.minusDays(1));

		insertNKeysPerDayInInterval(14,
				midnight.minusDays(4),
				now, now.minusDays(12));

		// request the keys with date date 1 day ago. no publish until.
		MockHttpServletResponse response = mockMvc
				.perform(get("/v1/gaen/exposed/"
						+ midnight.minusDays(8).getTimestamp())
								.header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29"))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse();

		Long publishedUntil = Long.parseLong(response.getHeader("X-PUBLISHED-UNTIL"));
		assertTrue(publishedUntil < now.getTimestamp());

		verifyZipResponse(response, 20);

		// request again the keys with date date 1 day ago. with publish until, so that
		// we only get the second batch.
		var bucketAfterSecondRelease = Duration.ofMillis(midnight.getTimestamp()).minusDays(1).plusHours(12).dividedBy(Duration.ofHours(2)) * 2*60*60*1000;
		MockHttpServletResponse responseWithPublishedAfter = mockMvc
				.perform(get("/v1/gaen/exposed/"
						+ midnight.minusDays(8).getTimestamp())
								.header("User-Agent", "ch.admin.bag.dp3t.dev;1.0.7;1595591959493;Android;29").param("publishedafter",
										Long.toString(bucketAfterSecondRelease)))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse();

		//we always have 10
		verifyZipResponse(responseWithPublishedAfter, 10);
		UTCInstant.resetClock();
    }
    

    private void verifyZipResponse(MockHttpServletResponse response, int expectKeyCount)
			throws Exception {
		ByteArrayInputStream baisOuter = new ByteArrayInputStream(response.getContentAsByteArray());
		ZipInputStream zipOuter = new ZipInputStream(baisOuter);
		ZipEntry entry = zipOuter.getNextEntry();
		boolean foundData = false;
		boolean foundSignature = false;

		byte[] signatureProto = null;
		byte[] exportBin = null;
		byte[] keyProto = null;

		while (entry != null) {
			if (entry.getName().equals("export.bin")) {
				foundData = true;
				exportBin = zipOuter.readAllBytes();
				keyProto = new byte[exportBin.length-16];
				System.arraycopy(exportBin, 16, keyProto, 0, keyProto.length);
			}
			if (entry.getName().equals("export.sig")) {
				foundSignature = true;
				signatureProto = zipOuter.readAllBytes();
			}
			entry = zipOuter.getNextEntry();
		}

		assertTrue(foundData);
		assertTrue(foundSignature);

		var list = TemporaryExposureKeyFormat.TEKSignatureList.parseFrom(signatureProto);
		var export = TemporaryExposureKeyFormat.TemporaryExposureKeyExport.parseFrom(keyProto);
		for(var key : export.getKeysList()) {
			assertNotEquals(0, key.getRollingPeriod());
		}
		var sig = list.getSignatures(0);
		java.security.Signature signatureVerifier = java.security.Signature
				.getInstance(sig.getSignatureInfo().getSignatureAlgorithm().trim());
		signatureVerifier.initVerify(signer.getPublicKey());

		signatureVerifier.update(exportBin);
		assertTrue(signatureVerifier.verify(sig.getSignature().toByteArray()));
		assertEquals(expectKeyCount, export.getKeysCount());
	}

	private void insertNKeysPerDayInIntervalWithDebugFlag(int n, UTCInstant start, UTCInstant end, UTCInstant receivedAt, boolean debug) throws Exception {
		var current = start;
		Map<Integer, Integer> rollingToCount = new HashMap<>();
		while (current.isBeforeExact(end)) {
			List<GaenKey> keys = new ArrayList<>();
			SecureRandom random = new SecureRandom();
			int lastRolling = (int)start.get10MinutesSince1970();
			for (int i = 0; i < n; i++) {
				GaenKey key = new GaenKey();
				byte[] keyBytes = new byte[16];
				random.nextBytes(keyBytes);
				key.setKeyData(Base64.getEncoder().encodeToString(keyBytes));
				key.setRollingPeriod(144);
				logger.info("Rolling Start number: " + lastRolling);
				key.setRollingStartNumber(lastRolling);
				key.setTransmissionRiskLevel(1);
				key.setFake(0);
				keys.add(key);
				
				Integer count = rollingToCount.get(lastRolling);
				if (count == null) {
					count = 0;
				}
				count = count + 1;
				rollingToCount.put(lastRolling, count);
				
				lastRolling -= Duration.ofDays(1).dividedBy(Duration.ofMinutes(10));
				
			}
			if(debug) {
				testGaenDataService.upsertExposeesDebug(keys, receivedAt);
			} else {
				testGaenDataService.upsertExposees(keys, receivedAt);
			}
			current = current.plusDays(1);
		}
		for (Entry<Integer, Integer> entry: rollingToCount.entrySet()) {
			logger.info("Rolling start number: " + entry.getKey() + " -> count: " + entry.getValue() + " (received at: " + receivedAt.toString() + ")");
		}
	}

	private void insertNKeysPerDayInInterval(int n, UTCInstant start, UTCInstant end, UTCInstant receivedAt)
			throws Exception {
		insertNKeysPerDayInIntervalWithDebugFlag(n, start, end, receivedAt, false);
	}
}