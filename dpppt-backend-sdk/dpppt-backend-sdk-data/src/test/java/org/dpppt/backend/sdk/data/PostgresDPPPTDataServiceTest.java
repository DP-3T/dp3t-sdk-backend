package org.dpppt.backend.sdk.data;

import org.assertj.core.api.Assertions;
import org.dpppt.backend.sdk.data.config.DPPPTDataServiceConfig;
import org.dpppt.backend.sdk.data.config.FlyWayConfig;
import org.dpppt.backend.sdk.data.config.PostgresDataConfig;
import org.dpppt.backend.sdk.model.Exposee;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.sql.DataSource;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {PostgresDataConfig.class, FlyWayConfig.class, DPPPTDataServiceConfig.class})
@ActiveProfiles("postgres")
public class PostgresDPPPTDataServiceTest {

    @Autowired
    private DPPPTDataService dppptDataService;

    @Autowired
    private DataSource dataSource;

    @After
    public void tearDown() throws SQLException {
        executeSQL("truncate table t_exposed");
        executeSQL("truncate table t_redeem_uuid");
    }

    @Test
    public void shouldAddAnExposee() throws SQLException {
        // GIVEN
        final long exposeeCountBefore = getExposeeCount();

        Exposee exposee = new Exposee();
        exposee.setKey("key1");
        exposee.setKeyDate(DateTime.parse("2014-01-28").withTimeAtStartOfDay().getMillis());

        // WHEN
        dppptDataService.upsertExposee(exposee, "test-app");

        // THEN
        final long exposeeCountAfter = getExposeeCount();
        Assertions.assertThat(exposeeCountAfter).isEqualTo(exposeeCountBefore + 1);

        try (
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement("select * from t_exposed t where t.key = 'key1'");
            final ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();

            Assertions.assertThat(resultSet.getInt("pk_exposed_id")).isPositive();
            Assertions.assertThat(resultSet.getString("key")).isEqualTo("key1");
            Assertions.assertThat(resultSet.getString("received_at")).isNotNull();
            Assertions.assertThat(resultSet.getString("app_source")).isEqualTo("test-app");
            Assertions.assertThat(resultSet.getDate("key_date")).isEqualTo(Date.valueOf(LocalDate.of(2014, 1, 28)));
        }
    }

    @Test
    public void shouldGetSortedExposedForDay() {

        // GIVEN
        {
            Exposee exposee = new Exposee();
            exposee.setKey("key1");
            exposee.setKeyDate(DateTime.parse("2014-01-28").withTimeAtStartOfDay().getMillis());

            dppptDataService.upsertExposee(exposee, "test-app");
        }

        {
            Exposee exposee = new Exposee();
            exposee.setKey("key2");
            exposee.setKeyDate(DateTime.parse("2014-01-29").withTimeAtStartOfDay().getMillis());

            dppptDataService.upsertExposee(exposee, "test-app");
        }

        // WHEN
        final List<Exposee> sortedExposedForDay = dppptDataService.getSortedExposedForDay(DateTime.now());

        // THEN
        Assertions.assertThat(sortedExposedForDay).hasSize(2);
        Assertions.assertThat(sortedExposedForDay.get(0).getKey()).isEqualTo("key2");
        Assertions.assertThat(sortedExposedForDay.get(1).getKey()).isEqualTo("key1");
    }

    @Test
    public void shouldReturnEmptyListForGetSortedExposedForDay() {

        // WHEN
        final List<Exposee> sortedExposedForDay = dppptDataService.getSortedExposedForDay(DateTime.now());

        // THEN
        Assertions.assertThat(sortedExposedForDay).isEmpty();
    }

    @Test
    public void shouldGetMaxExposedIdForDay() throws SQLException {

        // GIVEN
        {
            Exposee exposee = new Exposee();
            exposee.setKey("key100");
            exposee.setKeyDate(DateTime.parse("2014-01-28").withTimeAtStartOfDay().getMillis());

            dppptDataService.upsertExposee(exposee, "test-app");
        }

        {
            Exposee exposee = new Exposee();
            exposee.setKey("key200");
            exposee.setKeyDate(DateTime.parse("2014-01-29").withTimeAtStartOfDay().getMillis());

            dppptDataService.upsertExposee(exposee, "test-app");
        }

        // WHEN
        final Integer maxExposedIdForDay = dppptDataService.getMaxExposedIdForDay(DateTime.now());

        // THEN
        try (
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement("select * from t_exposed t where t.key = 'key200'");
            final ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();

            Assertions.assertThat(maxExposedIdForDay).isEqualTo(resultSet.getInt("pk_exposed_id"));
        }
    }

    @Test
    public void shouldGetZeroForGetMaxExposedIdForDay() {

        // WHEN
        final Integer maxExposedIdForDay = dppptDataService.getMaxExposedIdForDay(DateTime.now());

        // THEN
        Assertions.assertThat(maxExposedIdForDay).isEqualTo(0);
    }

    @Test
    public void testRedeemUUID() {
        boolean actual = dppptDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
        assertTrue(actual);
        actual = dppptDataService.checkAndInsertPublishUUID("bc77d983-2359-48e8-835a-de673fe53ccb");
        assertFalse(actual);
        actual = dppptDataService.checkAndInsertPublishUUID("1c444adb-0924-4dc4-a7eb-1f52aa6b9575");
        assertTrue(actual);
    }

    private long getExposeeCount() throws SQLException {
        try (
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement("select count(*) from t_exposed");
            final ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    protected void executeSQL(String sql) throws SQLException {
        try (
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ) {
            preparedStatement.execute();
        }
    }
}
