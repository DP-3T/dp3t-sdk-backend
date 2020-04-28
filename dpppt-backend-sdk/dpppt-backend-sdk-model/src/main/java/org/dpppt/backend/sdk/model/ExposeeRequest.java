/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;

public class ExposeeRequest {

	@NotNull
	private boolean fake;

	@NotNull
	private String key;

	@NotNull
	private long keyDate;

	private ExposeeAuthData authData;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public ExposeeAuthData getAuthData() {
		return authData;
	}

	public void setAuthData(ExposeeAuthData authData) {
		this.authData = authData;
	}

	public long getKeyDate() {
		return keyDate;
	}

	public void setKeyDate(long keyDate) {
		this.keyDate = keyDate;
	}
	public boolean isFake() {
		return fake;
	}

	public void setIsFake(boolean fake) {
		this.fake = fake;
	}
}
