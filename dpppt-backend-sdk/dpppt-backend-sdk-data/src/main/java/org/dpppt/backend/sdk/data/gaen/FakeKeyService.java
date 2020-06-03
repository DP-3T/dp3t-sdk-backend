package org.dpppt.backend.sdk.data.gaen;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.dpppt.backend.sdk.model.gaen.GaenUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeKeyService {
	
	private final GAENDataService dataService;
	private final Integer minNumOfKeys;
	private final SecureRandom random;
	private final Integer keySize;
	private final Duration retentionPeriod;
	private final boolean isEnabled;

	private static final Logger logger = LoggerFactory.getLogger(FakeKeyService.class);

	public FakeKeyService(GAENDataService dataService, Integer minNumOfKeys, Integer keySize, Duration retentionPeriod,
			boolean isEnabled) throws NoSuchAlgorithmException {
		this.dataService = dataService;
		this.minNumOfKeys = minNumOfKeys;
		this.random = new SecureRandom();
		this.keySize = keySize;
		this.retentionPeriod = retentionPeriod;
		this.isEnabled = isEnabled;
		this.updateFakeKeys();
	}

	public void updateFakeKeys() {
		deleteAllKeys();
		LocalDate currentKeyDate = LocalDate.now(ZoneOffset.UTC);
		var tmpDate = currentKeyDate.minusDays(retentionPeriod.toDays());
		logger.debug("Fill Fake keys. Start: " + currentKeyDate + " End: " + tmpDate);
		do {
			var keys = new ArrayList<GaenKey>();
			for (int i = 0; i < minNumOfKeys; i++) {
				byte[] keyData = new byte[keySize];
				random.nextBytes(keyData);
				var keyGAENTime = (int) Duration.ofSeconds(tmpDate.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC))
						.dividedBy(GaenUnit.TenMinutes.getDuration());
				var key = new GaenKey(Base64.getEncoder().encodeToString(keyData), keyGAENTime, 144, 0);
				keys.add(key);
			}
			this.dataService.upsertExposees(keys);
			tmpDate = tmpDate.plusDays(1);
		} while (tmpDate.isBefore(currentKeyDate.plusDays(1)));
	}

	private void deleteAllKeys() {
		logger.debug("Delete all fake keys");
		this.dataService.cleanDB(Duration.ofDays(0));
	}

	public List<GaenKey> fillUpKeys(List<GaenKey> keys, Long keyDate) {
		if (!isEnabled) {
			return keys;
		}
		var fakeKeys = this.dataService.getSortedExposedForKeyDate(keyDate, null,
				LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());

		keys.addAll(fakeKeys);
		return keys;
	}
}