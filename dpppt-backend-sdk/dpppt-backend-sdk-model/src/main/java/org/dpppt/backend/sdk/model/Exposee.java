/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ubique.openapi.docannotations.Documentation;

public class Exposee {
	@JsonIgnore
	private Integer Id;

	@NotNull
	@Documentation(description = "The SecretKey of a exposed as a base64 encoded string. The SecretKey consists of 32 bytes.", example = "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVpBQkNERUY=")
	private String key;

	@NotNull
	@Documentation(description = "The onset of an exposed.", example = "2020-04-06")
	private String onset;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@JsonIgnore
	public Integer getId() {
		return Id;
	}

	public void setId(Integer id) {
		Id = id;
	}

	public String getOnset() {
		return onset;
	}

	public void setOnset(String onset) {
		this.onset = onset;
	}
}
