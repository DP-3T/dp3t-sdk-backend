package org.dpppt.backend.sdk.data.gaen;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.dpppt.backend.sdk.model.gaen.GaenKeyWithOrigin;
import org.springframework.jdbc.core.RowMapper;

public class GaenKeyWithOriginRowMapper implements RowMapper<GaenKeyWithOrigin> {

  private final GaenKeyRowMapper simpleKeyRowMapper = new GaenKeyRowMapper();

  @Override
  public GaenKeyWithOrigin mapRow(ResultSet rs, int rowNum) throws SQLException {
    var gaenKey = simpleKeyRowMapper.mapRow(rs, rowNum);
    var gaenKeyWithOrigin = new GaenKeyWithOrigin();
    gaenKeyWithOrigin.setGaenKey(gaenKey);
    gaenKeyWithOrigin.setOrigin(rs.getString("origin"));
    gaenKeyWithOrigin.setId(rs.getString("pk_exposed_id"));
    return gaenKeyWithOrigin;
  }
}
