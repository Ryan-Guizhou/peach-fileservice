package com.peach.fileservice.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties extends Properties {

    private static final long serialVersionUID = 764910568046389618L;

    public void setJdbcUrl(String jdbcUrl) {
        this.setProperty("jdbcUrl", jdbcUrl);
    }

    public void setDriverClassName(String driverClassName) {
        this.setProperty("driverClassName", driverClassName);
    }
    public void setUsername(String username) {
        this.setProperty("username", username);
    }

    public void setPassword(String password) {
        this.setProperty("password", password);
    }

    public void setConnectionTimeout(String connectionTimeout) {
        this.setProperty("connectionTimeout", connectionTimeout);
    }

    public void setIdleTimeout(String idleTimeout) {
        this.setProperty("idleTimeout", idleTimeout);
    }

    public void setMaxLifetime(String maxLifetime) {
        this.setProperty("maxLifetime", maxLifetime);
    }

    public void setMaximumPoolSize(String maximumPoolSize) {
        this.setProperty("maximumPoolSize", maximumPoolSize);
    }

    public void setConnectionTestQuery(String connectionTestQuery) {
        this.setProperty("connectionTestQuery", connectionTestQuery);
    }
    public void setMinimumIdle(String minimumIdle){
        this.setProperty("minimumIdle",minimumIdle);
    }
}