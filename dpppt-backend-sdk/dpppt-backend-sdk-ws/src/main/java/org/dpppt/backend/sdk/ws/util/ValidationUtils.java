package org.dpppt.backend.sdk.ws.util;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

public class ValidationUtils {
    private final int KEY_LENGTH_BYTES;
    private final Duration retentionPeriod;
    public ValidationUtils(int keyLengthBytes, Duration retentionPeriod) {
        this.KEY_LENGTH_BYTES = keyLengthBytes;
        this.retentionPeriod = retentionPeriod;
    }
    public boolean isValidBase64Key(String value) {
		try {
			byte[] key = Base64.getDecoder().decode(value);
			if (key.length != KEY_LENGTH_BYTES) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
    }
    
    public boolean isDateInRange(OffsetDateTime timestamp){
        if (timestamp.isAfter(Instant.now().atOffset(ZoneOffset.UTC))) {
            return false;
        }
        if (timestamp.isBefore(Instant.now().atOffset(ZoneOffset.UTC).minus(retentionPeriod))) {
            return false;
        }
        return true;
    }
}