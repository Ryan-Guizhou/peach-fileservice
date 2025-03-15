package com.peach.fileservice.starter;


import com.peach.common.anno.MyBatisDao;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Indexed;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Slf4j
@Indexed
@Configuration
@ComponentScan(basePackages = {"com.peach.fileservice",}, lazyInit = true)
@MapperScan(lazyInitialization = "true", basePackages = "com.peach.fileservice.dao",
        annotationClass = MyBatisDao.class,sqlSessionFactoryRef = "mybatis-session")
public class FileserviceStarter {


    @Value("${knife4j.host:http://localhost:8888}")
    private String host;

    /**
     * @description 注册bean
     * @author pandasF
     * @date 2021/8/24 11:30
     * @param:
     * @return springfox.documentation.spring.web.plugins.Docket
     */
    @Lazy
    @Bean
    public Docket fileserviceApi() {
        Contact contact = new Contact("PEACH","https://github.com/Ryan-Guizhou","huanhuanshu48@gmail.com");
        Docket docket=new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("PEACH-API文档")
                        .description("PEACH-API文档")
                        .termsOfServiceUrl(host)
                        .contact(contact)
                        .version("PEACH-1.0.0")
                        .build())
                //分组名称
                .groupName("文件服务API")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.peach.fileservice"))
                .build();
        log.error("knife4j fileservice has been configured");
        return docket;
    }


}
