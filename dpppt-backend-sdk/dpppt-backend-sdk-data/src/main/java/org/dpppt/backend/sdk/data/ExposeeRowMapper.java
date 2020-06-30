/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package org.dpppt.backend.sdk.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.dpppt.backend.sdk.model.Exposee;
import org.springframework.jdbc.core.RowMapper;

public class ExposeeRowMapper implements RowMapper<Exposee> {
	@Override
	public Exposee mapRow(ResultSet rs, int rowNum) throws SQLException {
		Exposee exposee = new Exposee();
		exposee.setKey(rs.getString("key"));
		exposee.setId(rs.getInt("pk_exposed_id"));
		exposee.setKeyDate(rs.getTimestamp("key_date").getTime());

		String list = rs.getString("countries_visited");
		list = list.replace("[", "");
		list = list.replace("]", "");

		exposee.setCountryCodeList(new ArrayList<>(Arrays.asList(list)));
		return exposee;
	}
}