package org.dpppt.backend.sdk.data.gaen;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.springframework.jdbc.core.RowMapper;

public class GaenKeyRowMapper implements RowMapper<GaenKey> {

  @Override
  public GaenKey mapRow(ResultSet rs, int rowNum) throws SQLException {
    var gaenKey = new GaenKey();
    gaenKey.setKeyData(rs.getString("key"));
    gaenKey.setRollingStartNumber(rs.getInt("rolling_start_number"));
    gaenKey.setRollingPeriod(rs.getInt("rolling_period"));
    return gaenKey;
  }
}
