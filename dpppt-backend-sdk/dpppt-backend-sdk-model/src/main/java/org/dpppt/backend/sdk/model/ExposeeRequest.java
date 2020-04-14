/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import ch.ubique.openapi.docannotations.Documentation;

public class ExposeeRequest {

	@NotNull
	@Documentation(description = "The SecretKey used to generate EphID base64 encoded.", example = "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVpBQkNERUY=")
	private String key;

	@NotNull
	@Size(max = 10)
	@Documentation(description = "The onset date of the secret key. Format: yyyy-MM-dd", example = "2019-01-31")
	private String onset;

	@NotNull
	@Documentation(description = "AuthenticationData provided by the health institutes to verify the test results", example = "TBD")
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

	public String getOnset() {
		return onset;
	}

	public void setOnset(String onset) {
		this.onset = onset;
	}
}
