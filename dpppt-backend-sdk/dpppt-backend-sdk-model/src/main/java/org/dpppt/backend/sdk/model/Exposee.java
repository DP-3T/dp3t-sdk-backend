/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class Exposee {
	@JsonIgnore
	private Integer Id;

	@NotNull
	private String key;

	@NotNull
	private long keyDate;

	@JsonIgnore
	public Integer getId() {
		return Id;
	}
}
