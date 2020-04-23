/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import java.util.ArrayList;
import java.util.List;

public class ExposedOverview {

	private Long batchReleaseTime;

	private List<Exposee> exposed = new ArrayList<>();

	public ExposedOverview() {
	}

	public Long getBatchReleaseTime() {
		return batchReleaseTime;
	}

	public void setBatchReleaseTime(Long batchReleaseTime) {
		this.batchReleaseTime = batchReleaseTime;
	}

	public ExposedOverview(List<Exposee> exposed) {
		this.exposed = exposed;
	}

	public List<Exposee> getExposed() {
		return exposed;
	}

	public void setExposed(List<Exposee> exposed) {
		this.exposed = exposed;
	}
}
