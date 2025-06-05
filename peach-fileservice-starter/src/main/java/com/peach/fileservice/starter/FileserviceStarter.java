package com.peach.fileservice.starter;


import com.peach.common.anno.MyBatisDao;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
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

    /**
     * 注册文件服务模块接口文档
     * @return
     */
    @Lazy
    @Bean
    public Docket fileserviceApi() {
        Contact contact = new Contact("PEACH","https://github.com/Ryan-Guizhou","huanhuanshu48@gmail.com");
        Docket docket=new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("PEACH-API文档")
                        .description("PEACH-API文档")
                        .contact(contact)
                        .version("PEACH-1.0.0")
                        .build())
                //分组名称
                .groupName("FILE_API")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.peach.fileservice"))
                .build();
        log.info("knife4j FILE_API has been configured");
        return docket;
    }


}
