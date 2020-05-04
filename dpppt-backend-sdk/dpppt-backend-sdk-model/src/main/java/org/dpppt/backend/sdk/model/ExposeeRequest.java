/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;

public class ExposeeRequest {

	private Integer fake = 0;

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
	
	public Integer isFake() {
		return fake;
	}

	public void setIsFake(Integer fake) {
		this.fake = fake;
	}
}
