package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;

public class AuthorizationCode {

	@NotNull
	private String code;

	public AuthorizationCode() {}

	public AuthorizationCode(String code) {
		assert code != null;
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
