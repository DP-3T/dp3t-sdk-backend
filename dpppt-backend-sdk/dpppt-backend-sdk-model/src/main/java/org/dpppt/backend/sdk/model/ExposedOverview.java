/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.model;

import java.util.ArrayList;
import java.util.List;

import ch.ubique.openapi.docannotations.Documentation;

public class ExposedOverview {
	@Documentation(description = "A list of all SecretKeys", example = "c.f. Exposee model")
	List<Exposee> exposed = new ArrayList<>();

	public ExposedOverview() {
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
