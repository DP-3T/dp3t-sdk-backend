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

public class FakeKeyService {
	private final GAENDataService dataService;
	private final Integer minNumOfKeys;
	private LocalDate currentKeyDate;
	private final SecureRandom random;
	private final Integer keySize;
	private final Duration retentionPeriod;
	private final boolean isEnabled;

	public FakeKeyService(GAENDataService dataService, Integer minNumOfKeys, Integer keySize, Duration retentionPeriod,
			boolean isEnabled) throws NoSuchAlgorithmException {
		this.dataService = dataService;
		this.minNumOfKeys = minNumOfKeys;
		this.random = SecureRandom.getInstanceStrong();
		this.keySize = keySize;
		this.retentionPeriod = retentionPeriod;
		this.isEnabled = isEnabled;
		this.currentKeyDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);
		this.updateFakeKeys();
	}

	public void updateFakeKeys() {
		deleteAllKeys();
		currentKeyDate = currentKeyDate.plusDays(1);
		var tmpDate = currentKeyDate.minusDays(retentionPeriod.toDays());
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
		} while (tmpDate.isBefore(currentKeyDate));
	}

	private void deleteAllKeys() {
		this.dataService.cleanDB(retentionPeriod.plusDays(2));
	}

	public List<GaenKey> fillUpKeys(List<GaenKey> keys, Long keyDate) {
		if (!isEnabled || keys.size() >= minNumOfKeys) {
			return keys;
		}
		var fakeKeys = this.dataService.getSortedExposedForKeyDate(keyDate, null,
				LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
		if (fakeKeys.size() < minNumOfKeys) {
			return keys;
		}
		for (int i = minNumOfKeys - keys.size() - 1; i >= 0; i--) {
			keys.add(fakeKeys.get(i));
		}
		return keys;
	}
}