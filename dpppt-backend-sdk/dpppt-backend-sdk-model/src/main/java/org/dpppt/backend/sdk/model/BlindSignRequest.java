package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;

public class BlindSignRequest {

	@NotNull
	private String authorizationCode;

	@NotNull
	private String base64BlindSignRequest;

	public BlindSignRequest() {}

	public BlindSignRequest(AuthorizationCode authorizationCode, String base64BlindSignRequest) {
		assert authorizationCode != null;
		assert base64BlindSignRequest != null;
		this.authorizationCode = authorizationCode.getCode();
		this.base64BlindSignRequest = base64BlindSignRequest;
	}

	public String getAuthorizationCode() {
		return authorizationCode;
	}

	public String getBase64BlindSignRequest() {
		return base64BlindSignRequest;
	}
}
