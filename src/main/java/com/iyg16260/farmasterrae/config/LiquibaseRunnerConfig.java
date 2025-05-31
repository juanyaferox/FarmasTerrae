package com.iyg16260.farmasterrae.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Clase de configuración para evitar que Liquibase se ejecute antes que JPA Hibernate cree las tablas
 */
@Configuration
@EnableConfigurationProperties (LiquibaseProperties.class)
public class LiquibaseRunnerConfig {

    @Autowired
    DataSource dataSource;

    @Autowired
    LiquibaseProperties liquibaseProperties;

    @Bean
    public CommandLineRunner runLiquibase() {
        return args -> {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog(liquibaseProperties.getChangeLog());
            liquibase.setContexts(String.valueOf(liquibaseProperties.getContexts()));
            liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
            liquibase.setDropFirst(liquibaseProperties.isDropFirst());
            liquibase.setShouldRun(liquibaseProperties.isEnabled());
            liquibase.afterPropertiesSet();
        };
    }
}
