package com.peach.fileservice.starter;



import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.peach.common.anno.MyBatisDao;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Indexed;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Indexed
@Configuration
@ComponentScan(basePackages = {"com.peach.fileservice",}, lazyInit = true)
@MapperScan(lazyInitialization = "true", basePackages = "com.peach.fileservice.dao",
        annotationClass = MyBatisDao.class,sqlSessionFactoryRef = "mybatis-session")
public class FileserviceStarter {


//    @Lazy
//    @Bean
//    @ConditionalOnProperty(value = "pty.store.type", havingValue = "mongoDB")
//    public MongoClient mongoClient(@Value("${mongodb.uri}") String uri) {
//        MongoClient mongoClient = MongoClients.create(uri);
//        return mongoClient;
//    }


}
