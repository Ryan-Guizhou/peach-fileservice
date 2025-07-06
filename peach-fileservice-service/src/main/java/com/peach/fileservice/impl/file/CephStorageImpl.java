package com.peach.fileservice.impl.file;

import cn.hutool.core.io.FileUtil;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.google.common.collect.Lists;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.config.FileProperties;
import com.peach.fileservice.impl.AbstractFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/26 22:12
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "ceph")
public class CephStorageImpl extends AbstractFileStorageService {

    private final AmazonS3 s3Client;

    private final String bucketName;

    private final Integer urlTakeSign;

    private final String nginxProxy;

    public CephStorageImpl(FileProperties properties){
        String endpoint = properties.getS3().getEndpoint();
        String accessKey = properties.getS3().getAccessKey();
        String secretKey = properties.getS3().getSecretKey();
        String region = properties.getS3().getRegion();
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(basicAWSCredentials);
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .build();
        this.bucketName = properties.getS3().getBucketName();
        this.urlTakeSign = properties.getS3().getUrlTakeSign();
        this.nginxProxy = properties.getNginxProxy();
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        boolean flag = Boolean.TRUE;
        String sourceDirKey = normalizeDirectory(sourceDir);
        String targetDirKey = normalizeDirectory(targetDir);
        ObjectListing objectListing;
        try {
            // 删除目录及目录下的所有文件。
            String nextMarker = null;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName)
                        .withPrefix(sourceDirKey).withDelimiter(nextMarker);
                objectListing = s3Client.listObjects(listObjectsRequest);
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    String sourceKey = objectSummary.getKey();
                    try {
                        String targetKey = sourceKey.replace(sourceDirKey, targetDirKey);
                        if (StringUtils.isNotBlank(sourceKey)) {
                            CopyObjectRequest copyObjRequest = new CopyObjectRequest(bucketName, sourceKey, bucketName, targetKey);
                            s3Client.copyObject(copyObjRequest);
                        }
                    } catch (Exception e) {
                        flag = Boolean.FALSE;
                        log.error("copy object error！", e);
                    }
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (Exception e) {
            flag = Boolean.FALSE;
            log.error("copy object error！", e);
        }
        return flag;
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        try {
            String sourceDirKey = normalizeDirectory(sourceDir);
            String localDirKey = normalizeDirectory(localDir);
            String nextMarker = null;
            ObjectListing objectListing;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(sourceDirKey)
                        .withDelimiter(nextMarker);

                objectListing = s3Client.listObjects(listObjectsRequest);
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    String sourceKey = objectSummary.getKey();
                    String localKey = sourceKey.replace(sourceDirKey, localDirKey);
                    try (InputStream inputStream = this.getInputStreamByKey(sourceKey)) {
                        if (inputStream != null) {
                            FileUtil.writeFromStream(inputStream, localKey);
                        }
                    } catch (Exception e) {
                        log.error(sourceKey + " download failed " + e.getMessage());
                    }
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (Exception e) {
            log.error("download failed", e);
        }
        return FileUtil.exist(localDir);
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return upLoadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try(ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            upLoadInputStream(inputStream, targetPath, fileName);
        }catch (Exception e){
            log.error("upload failed,targetPath:[{}],fileName:[{}]", targetPath, fileName);
        }
        return StringUtil.EMPTY;
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        if (files == null || files.length == 0) {
            return Lists.newArrayList();
        }
        List<String> resultList = Lists.newArrayList();
        for (File file : files) {
            String url = upload(file, targetPath, file.getName());
            resultList.add(url);
        }
        return resultList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        try(FileInputStream inputStream = new FileInputStream(file)) {
            upLoadInputStream(inputStream, targetPath, fileName);
        }catch (Exception e){
            log.error("upload failed,targetPath:[{}],fileName:[{}]", targetPath, fileName);
        }
        return StringUtil.EMPTY;
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        String targetKey = getOssKey(targetPath);
        String localKey = buildKey(localPath,fileName);
        File file = null;
        try(InputStream inputStream = getInputStreamByKey(targetKey)){
            if (inputStream == null) {
                return Boolean.FALSE;
            }
            file = FileUtil.writeFromStream(inputStream, localKey);
        }catch (Exception e){
            log.error("download failed,targetPath:[{}],localPath:[{}],fileName:[{}]", targetKey,localKey, fileName);
        }
        return FileUtil.exist(file);
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        String key = buildKey(targetPath, fileName);
        String ossKey = getOssKey(key);
        return getInputStreamByKey(ossKey);
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        InputStream inputStream = null;
        String ossKey = getOssKey(key);
        try {
            inputStream = s3Client.getObject(bucketName, ossKey).getObjectContent();
            return inputStream;
        } catch (Exception e) {
            log.error("获取文件失败:[" + key + "]");
            ossKey = ossKey.replace(bucketName + "/", "");
            try {
                log.info("bucketName:[" + bucketName + "],replace key:[" + ossKey + "]");
                inputStream = s3Client.getObject(bucketName, ossKey).getObjectContent();
                return inputStream;
            } catch (Exception ex) {
                log.error("修改key 后获取文件失败:[" + ossKey + "] ", e);
            }
        }
        return null;
    }

    @Override
    public boolean delete(String key) {
        if (isHasIllegalChar(key)){
            log.error("delete file failed, [{}] is illegal,can't be deleted", key);
            return Boolean.FALSE;
        }
        boolean flag = Boolean.TRUE;
        try {
            key = removeUrlHost(key);
            String nextMarker = null;
            ObjectListing objectListing;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(handlerKeyPrefix(key))
                        .withDelimiter(nextMarker);
                objectListing = s3Client.listObjects(listObjectsRequest);
                if (!objectListing.getObjectSummaries().isEmpty()) {
                    List<String> keysToDelete = new ArrayList<>();
                    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                        String keyPath = objectSummary.getKey();
                        if (StringUtil.isNotBlank(keyPath)) {
                            keysToDelete.add(keyPath);
                        }
                    }
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
                            .withKeys(keysToDelete.toArray(new String[0]));
                    s3Client.deleteObjects(deleteObjectsRequest);
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        }catch (Exception e){
            log.error("delete file error,key is : [{}]",key,e);
            flag = Boolean.FALSE;
        }
        return flag;
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        return Boolean.FALSE;
    }

    @Override
    public String getUrlByKey(String key) {
        return getOrgUrlByKey(key,Boolean.FALSE);
    }

    @Override
    public String getPathByKey(String key) {
        return getOrgUrlByKey(key,Boolean.TRUE);
    }

    @Override
    public void setPublicReadAcl(String path) {
        try {
            s3Client.setObjectAcl(bucketName,getOssKey(path), CannedAccessControlList.PublicRead);
        }catch (Exception e){
            log.error("setPublicReadAcl error！,path:[{}]",path,e);
        }
    }

    /**
     * 文件流上传
     * @param inputStream
     * @param targetPath
     * @param fileName
     * @return
     */
    public String upLoadInputStream(InputStream inputStream, String targetPath, String fileName) {
        String url = null;
        String key = buildKey(targetPath , fileName);
        String keyPath = getOssKey(key);
        try(InputStream in = inputStream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[2048];
            int reader;
            while ((reader = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, reader);
            }
            buffer.flush();
            byte[] byteArray = buffer.toByteArray();
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(byteArray.length);
            s3Client.putObject(bucketName, keyPath, new ByteArrayInputStream(byteArray), objectMetadata);
            try {
                setPublicReadAcl(keyPath);
                log.info("set PublicRead success,keyPath : [{}]", keyPath);
            } catch (SdkClientException e) {
                log.error("set PublicRead failed:", e);
            }

            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, keyPath);
            String ossUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
            url = removeUrlHost(ossUrl);
            if (URL_TAKE_SIGN_NO == urlTakeSign) {
                url = url.split("\\?")[0];
            }
            return url;
        } catch (SdkClientException e) {
            log.error(e.getMessage());
        } catch (Exception e){
            log.error("upload OSS error！", e);
        }
        return null;
    }

    /**
     * 获取原始路径,其实就是通过可以获取到原始地址，如果isUrl为true的是话，那么就返回代理地址
     * @param key
     * @param isUrl 是否URL
     * @return 完整的原始地址
     */
    protected String getOrgUrlByKey(String key,boolean isUrl){
        String keyPath = getOssKey(key);
        String url = StringUtil.EMPTY;
        boolean flag = s3Client.doesObjectExist(bucketName, keyPath);
        try {
            if (!flag) {
                log.error("file does not exist,keyPath:[{}]",keyPath);
                return null;
            }
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, keyPath);
            generatePresignedUrlRequest.setExpiration(expiration);
            String ossUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
            if (ossUrl.contains(FileConstant.HTTPS_PREFIX)) {
                url = ossUrl.replaceAll(FileConstant.HTTPS_DOMAIN_REGEX, isUrl ? nginxProxy : StringUtil.EMPTY);
            } else {
                url.replaceAll(FileConstant.HTTP_DOMAIN_REGEX, StringUtil.EMPTY);
                url = ossUrl.replaceAll(FileConstant.HTTP_DOMAIN_REGEX,  isUrl ? nginxProxy : StringUtil.EMPTY);
            }
            if (urlTakeSign == URL_TAKE_SIGN_NO) {
                url = url.split("\\?")[0];
            }
            return url;
        } catch (Exception e) {
            log.error("getOrgUrlByKey error key:[{}],isUrl:[{}]",key,isUrl,e);
            return null;
        }

    }
}
