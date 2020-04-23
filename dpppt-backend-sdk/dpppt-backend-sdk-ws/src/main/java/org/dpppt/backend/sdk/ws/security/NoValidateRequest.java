/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.security;

import org.dpppt.backend.sdk.model.ExposeeRequest;

public class NoValidateRequest implements ValidateRequest {

	@Override
	public boolean isValid(Object authObject) {
		return true;
	}

	@Override
	public String getOnset(Object authObject, Object others) {
		if (others instanceof ExposeeRequest) {
			return ((ExposeeRequest) others).getOnset();
		}
		return "";
	}

}