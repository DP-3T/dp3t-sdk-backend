/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2020. All rights reserved.
 */

package org.dpppt.backend.sdk.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.dpppt.backend.sdk.model.Exposee;
import org.springframework.jdbc.core.RowMapper;

public class ExposeeRowMapper implements RowMapper<Exposee> {
	@Override
	public Exposee mapRow(ResultSet rs, int rowNum) throws SQLException {
		Exposee exposee = new Exposee();
		exposee.setKey(rs.getString("key"));
		exposee.setId(rs.getInt("pk_exposed_id"));
		exposee.setKeyDate(rs.getTimestamp("key_date").getTime());
		return exposee;
	}
}