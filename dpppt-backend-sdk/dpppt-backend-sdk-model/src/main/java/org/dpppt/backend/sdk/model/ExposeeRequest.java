/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ExposeeRequest {

	@NotNull
	private String key;

	@NotNull
	private long keyDate;

	private ExposeeAuthData authData;
}
