package org.dpppt.backend.sdk.data.gaen;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dpppt.backend.sdk.model.gaen.GaenKey;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

public class GaenKeyWithCountriesRowMapper
    implements ResultSetExtractor<List<GaenKeyWithCountries>> {

  @Override
  public List<GaenKeyWithCountries> extractData(ResultSet rs)
      throws SQLException, DataAccessException {
    Map<Integer, GaenKeyWithCountries> idToKey = new LinkedHashMap<>();
    GaenKeyRowMapper simpleKeyRowMapper = new GaenKeyRowMapper();
    int rowNum = 0;
    while (rs.next()) {
      Integer keyId = rs.getInt("pk_exposed_id");
      GaenKeyWithCountries key = idToKey.get(keyId);
      if (key == null) {
        key = new GaenKeyWithCountries();
        GaenKey gaenKey = simpleKeyRowMapper.mapRow(rs, rowNum);
        key.setGaenKey(gaenKey);
        key.setOrigin(rs.getString("origin"));
        idToKey.put(keyId, key);
      }
      key.getCountries().add(rs.getString("country"));
      rowNum++;
    }
    return new ArrayList<>(idToKey.values());
  }
}
