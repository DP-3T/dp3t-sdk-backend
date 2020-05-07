package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ExposedKey {
	@NotNull
	@Size(min = 24, max = 44)
	private String key;

	@NotNull
	private long keyDate;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public long getKeyDate() {
		return keyDate;
	}

	public void setKeyDate(long keyDate) {
		this.keyDate = keyDate;
	}
}