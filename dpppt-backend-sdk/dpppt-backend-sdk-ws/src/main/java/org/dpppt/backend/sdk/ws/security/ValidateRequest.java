/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.ws.security;

public interface ValidateRequest {
	
	public boolean isValid(Object authObject);

	// authObject is the Principal, given from Springboot
	// others can be any object (currently it is the ExposeeRequest, since we want
	// to allow no auth without the jwt profile)
	public long getKeyDate(Object authObject, Object others) throws InvalidDateException;

	public boolean isFakeRequest(Object authObject, Object others);
	
	public class InvalidDateException extends Exception {}
}