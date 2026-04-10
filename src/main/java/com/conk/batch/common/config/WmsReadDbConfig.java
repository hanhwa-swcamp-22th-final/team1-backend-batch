package com.conk.batch.common.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * WMS 읽기 전용 데이터소스를 구성한다.
 */
@Configuration
public class WmsReadDbConfig {

    @Bean
    @ConfigurationProperties("app.datasource.wms-read")
    public DataSourceProperties wmsReadDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "wmsReadDataSource")
    @ConfigurationProperties("app.datasource.wms-read.hikari")
    public DataSource wmsReadDataSource(
            @Qualifier("wmsReadDataSourceProperties") DataSourceProperties properties
    ) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setReadOnly(true);
        return dataSource;
    }

    @Bean
    public NamedParameterJdbcTemplate wmsReadNamedParameterJdbcTemplate(
            @Qualifier("wmsReadDataSource") DataSource wmsReadDataSource
    ) {
        return new NamedParameterJdbcTemplate(wmsReadDataSource);
    }
}
