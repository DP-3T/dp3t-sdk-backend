package org.dpppt.backend.sdk.model;

public enum HealthCondition {
	UNKNOWN(0), HEALTHY(1), EXPOSED(2), INFECTED(3), RECOVERED(4);

	public final int value;

	HealthCondition(int value) {
		this.value = value;
	}
}
