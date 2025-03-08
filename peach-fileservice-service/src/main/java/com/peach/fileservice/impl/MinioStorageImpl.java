package com.peach.fileservice.impl;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.peach.fileservice.AbstractFileStorageService;
import com.peach.fileservice.config.FileProperties;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/2/28 16:32
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "minio")
public class MinioStorageImpl extends AbstractFileStorageService {

    private final MinioClient minioClient;

    private final String bucketName;

    private final String minioEndpoint;

    public MinioStorageImpl(FileProperties properties) {
        String accessKey = properties.getMinio().getAccessKey();
        String secretKey = properties.getMinio().getSecretKey();
        String url = properties.getMinio().getUrl();
        this.bucketName = properties.getMinio().getBucketName();
        this.minioEndpoint = url;
        this.minioClient = MinioClient.builder().endpoint(url)
                .credentials(accessKey,secretKey).build();
     }
    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        return false;
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        return false;
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        log.error("开始上传文件");
        try {
            UploadObjectArgs uploadArgs = UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(targetPath)
                    .filename(fileName)
                    .build();
            ObjectWriteResponse response = minioClient.uploadObject(uploadArgs);
            return response.object();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
//        return "";
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        return "";
    }

    @Override
    public List<String> upload(File[] file, String targetPath) {
        return Collections.emptyList();
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        try {
            // 1. 判断桶是否存在
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build();
            boolean flag = minioClient.bucketExists(bucketExistsArgs);

            // 2. 如果桶不存在，则创建桶
            if (!flag) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' created successfully.", bucketName);
            }

            InputStream input = new FileInputStream(file);
            // 3. 上传文件
            String objectPath = targetPath + "/" + fileName; // 确保路径正确
            PutObjectArgs uploadArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)  // MinIO 中的对象路径
                    .stream(input, input.available(), -1) // 本地文件路径
                    .build();
            ObjectWriteResponse response = minioClient.putObject(uploadArgs);

            log.info("File '{}' uploaded successfully to bucket '{}'.", file.getName(), bucketName);

            // 生成预签名URL
            return getPresignedUrl(bucketName, objectPath);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to upload file to MinIO: {}", e.getMessage(), e);
        }
        return ""; // 返回空字符串表示上传失败
    }

    public String getPresignedUrl(String bucketName, String objectName) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(60 * 60)  // 链接有效期（秒），这里设置 1 小时
                    .build();
            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        return false;
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        return null;
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        return null;
    }

    @Override
    public boolean delete(String key) {
        RemoveObjectArgs builder = RemoveObjectArgs.builder().bucket(bucketName).object(key).build();
        try {
            minioClient.removeObject(builder);
            return true;
        } catch (Exception ex ) {
            log.error("delete objects failed");
        }
        return false;
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        return false;
    }

    @Override
    public String getUrlByKey(String key) {
        return "";
    }

    @Override
    public String getPathByKey(String key) {
        return "";
    }

    @Override
    public void setPublicReadAcl(String path) {

    }
}
