package org.dpppt.backend.sdk.data.gaen;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

public class DebugGaenKeyResultSetExtractor implements ResultSetExtractor<Map<String, List<GaenKey>>> {

	@Override
	public Map<String, List<GaenKey>> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, List<GaenKey>> result = new HashMap<String, List<GaenKey>>();
		GaenKeyRowMapper gaenKeyRowMapper = new GaenKeyRowMapper();
		while (rs.next()) {
			String deviceName = rs.getString("device_name");
			List<GaenKey> keysForDevice = result.get(deviceName);
			if (keysForDevice == null) {
				keysForDevice = new ArrayList<>();
				result.put(deviceName, keysForDevice);
			}
			GaenKey gaenKey = gaenKeyRowMapper.mapRow(rs, rs.getRow());
			keysForDevice.add(gaenKey);
		}
		return result;
	}

}
