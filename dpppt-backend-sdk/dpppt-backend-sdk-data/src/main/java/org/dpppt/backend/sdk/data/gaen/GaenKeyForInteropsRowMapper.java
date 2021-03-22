package org.dpppt.backend.sdk.data.gaen;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.dpppt.backend.sdk.model.gaen.GaenKeyForInterops;
import org.dpppt.backend.sdk.model.gaen.ReportType;
import org.dpppt.backend.sdk.utils.UTCInstant;
import org.springframework.jdbc.core.RowMapper;

public class GaenKeyForInteropsRowMapper implements RowMapper<GaenKeyForInterops> {

  private final GaenKeyRowMapper simpleKeyRowMapper = new GaenKeyRowMapper();

  @Override
  public GaenKeyForInterops mapRow(ResultSet rs, int rowNum) throws SQLException {
    var gaenKey = simpleKeyRowMapper.mapRow(rs, rowNum);
    var gaenKeyForInterops = new GaenKeyForInterops();
    gaenKeyForInterops.setGaenKey(gaenKey);
    gaenKeyForInterops.setOrigin(rs.getString("origin"));
    gaenKeyForInterops.setId(rs.getInt("pk_exposed_id"));
    String reportTypeValue = rs.getString("report_type");
    if (reportTypeValue != null) {
      gaenKeyForInterops.setReportType(ReportType.valueOf(reportTypeValue));
    }
    int daysSinceOnsetOfSymptoms = rs.getInt("days_since_onset_of_symptoms");
    if (!rs.wasNull()) {
      gaenKeyForInterops.setDaysSinceOnsetOfSymptoms(daysSinceOnsetOfSymptoms);
    }
    Timestamp receivedAtTimestamp = rs.getTimestamp("received_at");
    UTCInstant receivedAt = UTCInstant.ofEpochMillis(receivedAtTimestamp.getTime());
    gaenKeyForInterops.setReceivedAt(receivedAt);
    return gaenKeyForInterops;
  }
}
