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

import javax.validation.constraints.NotNull;

public class Application {

	@NotNull
	private String appId;
	@NotNull
	private String description;
	@NotNull
	private String backendBaseUrl;
	@NotNull
	private String listBaseUrl;
	@NotNull
	private String bleGattGuid;
	@NotNull
	private String contact;

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getBackendBaseUrl() {
		return backendBaseUrl;
	}

	public void setBackendBaseUrl(String backendBaseUrl) {
		this.backendBaseUrl = backendBaseUrl;
	}

	public String getListBaseUrl() {
		return listBaseUrl;
	}

	public void setListBaseUrl(String listBaseUrl) {
		this.listBaseUrl = listBaseUrl;
	}

	public String getBleGattGuid() {
		return bleGattGuid;
	}

	public void setBleGattGuid(String bleGattGuid) {
		this.bleGattGuid = bleGattGuid;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

}
