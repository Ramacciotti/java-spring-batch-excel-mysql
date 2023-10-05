package com.ramacciotti.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource connectToDatabase() {

        log.info("-------------------------------------------------------------------------------------------------------------------");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        try {

            log.info("** Trying to connect to the MysqlSQL database...");

            dataSource.getConnection().close();

            log.info("** Connected successfully!");

        } catch (Exception e) {

            throw new RuntimeException("## Ops! Couldn´t connect: " + e.getMessage(), e);

        }

        return dataSource;
    }

    @Bean
    public PlatformTransactionManager dataSourceTransactionManager(@Qualifier("connectToDatabase") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}