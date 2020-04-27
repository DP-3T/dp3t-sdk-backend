/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ExposedOverview {

	private Long batchReleaseTime;

	private List<Exposee> exposed = new ArrayList<>();

	public ExposedOverview(List<Exposee> exposed) {
		this.exposed = exposed;
	}
}
