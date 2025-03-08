package com.peach.fileservice.config;

import com.aliyun.oss.OSSClient;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.peach.common.util.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/9 15:13
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "file-storage")
public class FileProperties {

    /**
     * 存储类型
     */
    private String type;

    /**
     * monio相关配置
     */
    private MinioConfig minio;

    /**
     * cos相关配置
     */
    private CosConfig cos;

    /**
     * oss相关配置
     */
    private OssConfig oss;

    /**
     * mongo相关配置
     */
    private MongoConfig mongo;

    /**
     * s3相关配置
     */
    private S3Config s3;


    @Data
    public static class MinioConfig {

        private String url;

        private String accessKey;

        private String secretKey;

        private String bucketName;

    }

    @Data
    public static class CosConfig {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private String bucketName;

        private String region;
    }

    @Data
    public static class OssConfig {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private String bucketName;

        private String region;
    }

    @Data
    public static class MongoConfig {

        private String uri;

        private String dbName;
    }

    @Data
    public static class S3Config {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private String bucketName;

        private String region;

        private Integer urlTakeSign;
    }


}

