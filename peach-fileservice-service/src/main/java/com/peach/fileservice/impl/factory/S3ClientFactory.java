package com.peach.fileservice.impl.factory;

import cn.hutool.extra.spring.SpringUtil;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.peach.common.constant.PubCommonConst;
import com.peach.fileservice.config.FileProperties;
import com.peach.fileservice.impl.upload.ClosableS3Client;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/20 16:46
 */
@Slf4j
public class S3ClientFactory {

    public static ClosableS3Client getS3Client() {
        FileProperties fileProperties = SpringUtil.getBean(FileProperties.class);
        FileProperties.S3Config s3Config = fileProperties.getS3();
        String endpoint = s3Config.getEndpoint();
        String accessKey = s3Config.getAccessKey();
        String secretKey = s3Config.getSecretKey();
        String region = s3Config.getRegion();
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKey, secretKey));
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withPathStyleAccessEnabled(PubCommonConst.TRUE)
                .withChunkedEncodingDisabled(PubCommonConst.TRUE)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .build();
        return new ClosableS3Client(s3Client);
    }
}
