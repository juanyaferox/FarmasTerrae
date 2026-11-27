package com.iyg16260.farmasterrae.config;

import liquibase.integration.spring.SpringLiquibase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class LiquibaseRunnerConfigTest {

    private static final String CHANGELOG = "classpath:/db/changelog/data-only-changelog.yaml";

    private final DataSource dataSource = mock(DataSource.class);

    @Test
    void leavesContextsUnsetWhenNoneAreConfigured() {
        LiquibaseProperties properties = new LiquibaseProperties();
        properties.setContexts(null);

        SpringLiquibase liquibase = LiquibaseRunnerConfig.createLiquibase(dataSource, properties);

        assertThat(liquibase.getContexts()).isNull();
    }

    @Test
    void leavesContextsUnsetWhenTheListIsEmpty() {
        LiquibaseProperties properties = new LiquibaseProperties();
        properties.setContexts(List.of());

        SpringLiquibase liquibase = LiquibaseRunnerConfig.createLiquibase(dataSource, properties);

        assertThat(liquibase.getContexts()).isNull();
    }

    @Test
    void joinsConfiguredContexts() {
        LiquibaseProperties properties = new LiquibaseProperties();
        properties.setContexts(List.of("dev", "prod"));

        SpringLiquibase liquibase = LiquibaseRunnerConfig.createLiquibase(dataSource, properties);

        assertThat(liquibase.getContexts()).isEqualTo("dev,prod");
    }

    @Test
    void copiesTheOtherLiquibaseProperties() {
        LiquibaseProperties properties = new LiquibaseProperties();
        properties.setChangeLog(CHANGELOG);
        properties.setDefaultSchema("farmasterrae");
        properties.setDropFirst(true);
        properties.setAnalyticsEnabled(false);

        SpringLiquibase liquibase = LiquibaseRunnerConfig.createLiquibase(dataSource, properties);

        assertThat(liquibase.getDataSource()).isSameAs(dataSource);
        assertThat(liquibase.getChangeLog()).isEqualTo(CHANGELOG);
        assertThat(liquibase.getDefaultSchema()).isEqualTo("farmasterrae");
        assertThat(liquibase.isDropFirst()).isTrue();
        assertThat(liquibase.getAnalyticsEnabled()).isFalse();
    }

    @Test
    void doesNotTouchTheDatabaseWhenLiquibaseIsDisabled() throws Exception {
        LiquibaseProperties properties = new LiquibaseProperties();
        properties.setChangeLog(CHANGELOG);
        properties.setEnabled(false);

        new LiquibaseRunnerConfig().runLiquibase(dataSource, properties).run();

        verifyNoInteractions(dataSource);
    }

    static Stream<Arguments> nonDevContexts() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(List.of()),
                Arguments.of(List.of("prod")));
    }

    /**
     * Only the {@code profiles} table exists, so the run would fail if any demo changeset
     * (users, products, orders...) were applied.
     */
    @ParameterizedTest
    @MethodSource ("nonDevContexts")
    void realChangelogOnlySeedsProfilesOutsideTheDevContext(List<String> contexts) throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:liquibase-" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        try (Connection connection = h2.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE profiles (id BIGINT PRIMARY KEY, description VARCHAR(255), type VARCHAR(255))");
        }
        LiquibaseProperties properties = new LiquibaseProperties();
        properties.setChangeLog(CHANGELOG);
        properties.setContexts(contexts);
        properties.setAnalyticsEnabled(false);

        new LiquibaseRunnerConfig().runLiquibase(h2, properties).run();

        List<String> profiles = new ArrayList<>();
        try (Connection connection = h2.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT type FROM profiles ORDER BY id")) {
            while (rows.next()) {
                profiles.add(rows.getString(1));
            }
        }
        assertThat(profiles).containsExactly("ADMIN", "USER");
    }
}
