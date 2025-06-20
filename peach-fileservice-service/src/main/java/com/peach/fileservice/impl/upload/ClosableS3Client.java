package com.peach.fileservice.impl.upload;

import com.amazonaws.services.s3.AmazonS3;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/20 16:51
 */
@Slf4j
public class ClosableS3Client implements AutoCloseable {

    private final AmazonS3 s3Client;

    public ClosableS3Client(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public AmazonS3 getS3Client() {
        return s3Client;
    }

    @Override
    public void close() throws Exception {
        if (s3Client != null) {
            try {
                s3Client.shutdown();
            } catch (Exception e) {
                // 日志记录而不是抛出，避免影响主流程
                log.warn("S3 client shutdown error: {}", e.getMessage(), e);
            }
        }
    }
}
