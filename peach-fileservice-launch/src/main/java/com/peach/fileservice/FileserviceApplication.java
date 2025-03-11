package com.peach.fileservice;


import com.peach.fileservice.datasource.DataSourceProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.MultipartConfigElement;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 16 10月 2024 00:00
 */
@Slf4j
@EnableAsync
@EnableCaching
@EnableScheduling
@ComponentScan(basePackages = "com.peach.*")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, MongoAutoConfiguration.class})
public class FileserviceApplication implements WebMvcConfigurer {
    public static void main(String[] args) {
        new SpringApplicationBuilder(FileserviceApplication.class)
                .bannerMode(Banner.Mode.CONSOLE)
                .web(WebApplicationType.SERVLET)
                .run(args);
        log.info("FileServiceApplication has been started ...");
    }




    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // 单个数据大小
        factory.setMaxFileSize(DataSize.parse("102400KB")); // KB,MB
        // 总上传数据大小
        factory.setMaxRequestSize(DataSize.parse("1024000KB"));
        return factory.createMultipartConfig();
    }

    @Bean("datasource")
    @Primary
    public DataSource dataSource(DataSourceProperties dsp) {
        HikariConfig config = new HikariConfig(dsp);
        //禁止自动提交
        config.setAutoCommit(false);
        return new HikariDataSource(config);
    }

    @Bean("mybatis-session")
    @Primary
    public SqlSessionFactoryBean sqlSessionFactoryBean(@Qualifier("datasource") DataSource dataSource, DatabaseIdProvider idProvider) {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDatabaseIdProvider(idProvider);
        factory.setConfigLocation(new DefaultResourceLoader().getResource("classpath:mybatis-config.xml"));
        factory.setDataSource(dataSource);
        return factory;
    }

    @Bean("transactionManager")
    @Primary
    public PlatformTransactionManager ptyTxManager(@Qualifier("datasource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean("transactionTemplate")
    @Primary
    public TransactionTemplate ptyTransactionTemplateBean(
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate;
    }

    /**
     * 自动识别使用的数据库类型
     * 在mapper.xml中databaseId的值就是跟这里对应，
     * 如果没有databaseId选择则说明该sql适用所有数据库
     */
    @Bean
    public DatabaseIdProvider getDatabaseIdProvider() {
        DatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("Oracle", "oracle");
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("DB2", "db2");
        properties.setProperty("Derby", "derby");
        properties.setProperty("H2", "h2");
        properties.setProperty("HSQL", "hsql");
        properties.setProperty("Informix", "informix");
        properties.setProperty("MS-SQL", "ms-sql");
        properties.setProperty("PostgreSQL", "postgresql");
        properties.setProperty("Sybase", "sybase");
        properties.setProperty("Hana", "hana");
        properties.setProperty("DM", "oracle");
        databaseIdProvider.setProperties(properties);
        return databaseIdProvider;
    }



}
