/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class Application {

	@NotNull
	private String appId;
	@NotNull
	private String description;
	@NotNull
	private String backendBaseUrl;
	@NotNull
	private String listBaseUrl;
	@NotNull
	private String bleGattGuid;
	@NotNull
	private String contact;
}
