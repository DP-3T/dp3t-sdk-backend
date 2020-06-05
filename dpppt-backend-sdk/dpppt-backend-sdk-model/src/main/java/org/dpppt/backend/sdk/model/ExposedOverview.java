/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
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
