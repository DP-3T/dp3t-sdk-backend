/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;

public class ExposeeAuthData {

	@NotNull
	private String base64Value;

	@NotNull
	private String base64Signature;

	public ExposeeAuthData() {}

	public ExposeeAuthData(String base64Value, String base64signature) {
		assert base64Value != null;
		assert base64signature != null;
		this.base64Value = base64Value;
		this.base64Signature = base64signature;
	}

	public String getBase64Value() {
		return base64Value;
	}

	public String getBase64Signature() {
		return base64Signature;
	}
}
