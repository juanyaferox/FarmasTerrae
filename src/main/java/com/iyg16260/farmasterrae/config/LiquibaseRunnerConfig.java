package com.iyg16260.farmasterrae.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.util.List;

/**
 * Clase de configuración para evitar que Liquibase se ejecute antes que JPA Hibernate cree las tablas.
 * <p>
 * The data-only changelog runs from a {@link CommandLineRunner}, i.e. once Hibernate has created the
 * schema, and before any later runner such as {@link AdminBootstrapRunner}.
 */
@Configuration
@EnableConfigurationProperties (LiquibaseProperties.class)
public class LiquibaseRunnerConfig {

    /** Runs before every other runner so they can rely on the seeded profiles. */
    public static final int RUNNER_ORDER = Ordered.HIGHEST_PRECEDENCE;

    @Bean
    @Order (RUNNER_ORDER)
    public CommandLineRunner runLiquibase(DataSource dataSource, LiquibaseProperties liquibaseProperties) {
        return args -> createLiquibase(dataSource, liquibaseProperties).afterPropertiesSet();
    }

    /**
     * Builds the {@link SpringLiquibase} instance without running it.
     * <p>
     * Contexts are only set when configured: {@link LiquibaseProperties#getContexts()} is a
     * {@code List<String>}, and passing {@code String.valueOf(null)} would make Liquibase filter
     * on a literal {@code "null"} context.
     */
    static SpringLiquibase createLiquibase(DataSource dataSource, LiquibaseProperties properties) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        List<String> contexts = properties.getContexts();
        if (contexts != null && !contexts.isEmpty()) {
            liquibase.setContexts(String.join(",", contexts));
        }
        liquibase.setDefaultSchema(properties.getDefaultSchema());
        liquibase.setDropFirst(properties.isDropFirst());
        liquibase.setShouldRun(properties.isEnabled());
        if (properties.getAnalyticsEnabled() != null) {
            liquibase.setAnalyticsEnabled(properties.getAnalyticsEnabled());
        }
        return liquibase;
    }
}
