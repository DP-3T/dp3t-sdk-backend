package org.dpppt.backend.sdk.data.interops;

import java.time.LocalDate;
import javax.sql.DataSource;
import org.dpppt.backend.sdk.model.interops.FederationSyncLogEntry;
import org.dpppt.backend.sdk.model.interops.SyncAction;
import org.dpppt.backend.sdk.model.interops.SyncState;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

public class JdbcSyncLogDataServiceImpl implements SyncLogDataService {
  private static final String PGSQL = "pgsql";
  private final String dbType;
  private final NamedParameterJdbcTemplate jt;
  private final SimpleJdbcInsert logEntryInsert;

  public JdbcSyncLogDataServiceImpl(String dbType, DataSource dataSource) {
    this.dbType = dbType;
    this.jt = new NamedParameterJdbcTemplate(dataSource);
    this.logEntryInsert =
        new SimpleJdbcInsert(dataSource)
            .withTableName("t_federation_sync_log")
            .usingGeneratedKeyColumns("pk_federation_sync_log_id");
  }

  @Override
  public String getLatestBatchTagForDay(LocalDate uploadDate) {
    String sql =
        "select batch_tag from t_federation_sync_log"
            + " where upload_date = :upload_date"
            + " and action = :action"
            + " and state = :state"
            + " order by end_time desc"
            + " limit 1";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("upload_date", java.sql.Date.valueOf(uploadDate));
    params.addValue("action", SyncAction.DOWNLOAD.name());
    params.addValue("state", SyncState.DONE.name());
    try {
      return jt.queryForObject(sql, params, String.class);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  @Override
  public void insertLogEntry(FederationSyncLogEntry logEntry) {
    logEntryInsert.execute(getParams(logEntry));
  }

  private SqlParameterSource getParams(FederationSyncLogEntry logEntry) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("gateway", logEntry.getGateway());
    params.addValue("action", logEntry.getAction().name());
    params.addValue("batch_tag", logEntry.getBatchTag());
    params.addValue("upload_date", java.sql.Date.valueOf(logEntry.getUploadDate()));
    params.addValue("start_time", logEntry.getStartTime().getDate());
    params.addValue("end_time", logEntry.getEndTime().getDate());
    params.addValue("state", logEntry.getState().name());
    return params;
  }
}
