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
import com.peach.common.constant.PubCommonConst;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/9 16:38
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "s3")
public class S3StorageImpl extends AbstractFileStorageService {

    private final AmazonS3 s3Client;

    private final String bucketName;

    private final Integer urlTakeSign;

    private final String nginxProxy;

    public S3StorageImpl(FileProperties properties){
        String endpoint = properties.getS3().getEndpoint();
        String accessKey = properties.getS3().getAccessKey();
        String secretKey = properties.getS3().getSecretKey();
        String region = properties.getS3().getRegion();
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(basicAWSCredentials);
        this.s3Client =AmazonS3ClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withPathStyleAccessEnabled(false)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .build();
        this.bucketName = properties.getS3().getBucketName();
        this.urlTakeSign = properties.getS3().getUrlTakeSign();
        this.nginxProxy = properties.getNginxProxy();
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        String sourceDirKey = handlerKeyPrefix(sourceDir);
        String targetDirKey = handlerKeyPrefix(targetDir);
        String nextMarker = null;
        ObjectListing objectListing;
        boolean flag = true;
        try {
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(sourceDirKey)
                        .withMarker(nextMarker); // 这里要用 `withMarker()`

                objectListing = s3Client.listObjects(listObjectsRequest);
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    String sourceKey = objectSummary.getKey();
                    String targetKey = sourceKey.replace(sourceDirKey, targetDirKey);
                    try {
                        if (StringUtils.isNotBlank(sourceKey)) {
                            CopyObjectRequest copyObjRequest = new CopyObjectRequest(bucketName, sourceKey, bucketName, targetKey);
                            s3Client.copyObject(copyObjRequest);
                        }
                    }catch (Exception ex){
                       flag = false;
                       log.error("copy object error",ex);
                    }
                }
                nextMarker = objectListing.getNextMarker();
            }while (objectListing.isTruncated());
        }catch (Exception ex){
            flag = false;
            log.error("copy object error",ex);
        }
        return flag;
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        try {
            String sourceDirKey = handlerKeyPrefix(sourceDir);
            String localDirKey = handlerKeyPrefix(localDir);
            String nextMarker = null;
            ObjectListing objectListing;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(sourceDirKey)
                        .withMarker(nextMarker); // 这里要用 `withMarker()`

                objectListing = s3Client.listObjects(listObjectsRequest);

                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    String sourceKey = objectSummary.getKey();

                    // ** 判断是否是目录**
                    if (sourceKey.equals(sourceDirKey) || sourceKey.endsWith("/") || objectSummary.getSize() == 0) {
                        log.info("Skipping directory: {}", sourceKey);
                        FileUtil.mkdir(localDirKey + sourceKey.substring(sourceDirKey.length()));
                        continue;
                    }

                    String localFilePath = localDirKey + sourceKey.substring(sourceDirKey.length());

                    try (InputStream inputStream = getInputStreamByKey(sourceKey)) {
                        if (inputStream != null) {
                            File localFile = new File(localFilePath);
                            FileUtil.writeFromStream(inputStream, localFile);
                            log.info("Downloaded: {}", sourceKey);
                        } else {
                            log.warn("Skipping empty file: {}", sourceKey);
                        }
                    } catch (Exception e) {
                        log.error("Failed to download {}: {}", sourceKey, e.getMessage(), e);
                    }
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (Exception e) {
            log.error("Download directory failed: {}", e.getMessage(), e);
            return false;
        }
        return FileUtil.exist(localDir);
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return uploadInputStream(inputStream,targetPath,fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        ByteArrayInputStream byteArrayInputStream = null;
        byteArrayInputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return uploadInputStream(byteArrayInputStream,targetPath,fileName);
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        if (files == null){
            return Collections.emptyList();
        }
        List<String> urlList = Lists.newArrayList();
        for (File file : files) {
            String url = upload(file,targetPath,FileUtil.getName(file));
            urlList.add(url);
        }
        return urlList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        FileInputStream  inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            log.error("file is not exists");
        }
        return uploadInputStream(inputStream,targetPath,fileName);

    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        String downPath;
        try(InputStream inputStream = getInputStreamByKey(targetPath)){
            downPath = buildKey(localPath,fileName);
            if (inputStream != null) {
                FileUtil.writeFromStream(inputStream, downPath);
            }
        } catch (Exception e) {
            log.error("download file from s3 error！", e);
            return Boolean.FALSE;
        }
        return FileUtil.exist(downPath);
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        InputStream inputStream = null;
        String ossKey = null;
        try {
            String decodePath = URLDecoder.decode(targetPath, PubCommonConst.UTF_8);
            ossKey = targetPath.contains(fileName) ? decodePath : buildKey(decodePath,fileName);
            inputStream = getInputStreamByKey(ossKey);
            return inputStream;
        }catch (Exception ex){
            log.error("获取文件失败:[" + targetPath + "]");
        }
        return null;
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        InputStream inputStream = null;
        String ossKey = key;
        try {
            ossKey = URLDecoder.decode(ossKey, PubCommonConst.UTF_8);
            inputStream = s3Client.getObject(bucketName, getOssKey(ossKey)).getObjectContent();
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
            log.error("delete file failed, key is illegal");
            return Boolean.TRUE;
        }
        boolean flag = true;
        //真实存储路径 前缀
        try {
            key = removeUrlHost(key);
            String nextMarker = null;
            ObjectListing objectListing;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName)
                        .withPrefix(handlerKeyPrefix(key)).withDelimiter(nextMarker);
                objectListing = s3Client.listObjects(listObjectsRequest);
                if (!objectListing.getObjectSummaries().isEmpty()) {
                    List<String> keysToDelete = new ArrayList<>();
                    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                        String keyPath = objectSummary.getKey();
                        if (StringUtils.isNotBlank(keyPath)) {
                            keysToDelete.add(keyPath);
                        }
                    }
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
                            .withKeys(keysToDelete.toArray(new String[0]));
                    s3Client.deleteObjects(deleteObjectsRequest);
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (Exception e) {
            log.error("delete file field"+e.getMessage(), e);
            flag = Boolean.FALSE;
        }
        return flag;
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        return false;
    }

    @Override
    public String getUrlByKey(String key) {
        return getOrgUrlByKey(key,Boolean.TRUE);
    }

    @Override
    public String getPathByKey(String key) {
        return getOrgUrlByKey(key,Boolean.FALSE);
    }


    @Override
    public void setPublicReadAcl(String path) {
        try {
            path = URLDecoder.decode(path, PubCommonConst.UTF_8);
            s3Client.setObjectAcl(bucketName,getOssKey(path), CannedAccessControlList.PublicRead);
        } catch (Exception e) {
            log.error("setPublicReadAcl field"+e.getMessage(), e);
        }
    }

    /**
     * 获取原始文件url
     *
     * @param key
     * @return
     */
    protected String getOrgUrlByKey(String key, boolean isUrl) {
      
        String url = StringUtils.EMPTY;
        key =  handlerKeyPrefix(key);
        String keyPath = getOssKey(key);
        boolean flag = s3Client.doesObjectExist(bucketName, keyPath);
        try {
            if (!flag) {
                log.error("file does not exist,keyPath:[{}]",keyPath);   
                return StringUtils.EMPTY;
            }
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, keyPath);
            generatePresignedUrlRequest.setExpiration(expiration);
            String ossUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
            if (ossUrl.contains(FileConstant.HTTPS_DOMAIN_REGEX)) {
                url = ossUrl.replaceAll(FileConstant.HTTPS_DOMAIN_REGEX, StringUtils.EMPTY);
            }
            if (ossUrl.contains(FileConstant.HTTP_DOMAIN_REGEX)) {
                url = ossUrl.replaceAll(FileConstant.HTTP_DOMAIN_REGEX,  StringUtils.EMPTY);
            }
            if (urlTakeSign == URL_TAKE_SIGN_NO) {
                url = url.split("\\?")[0];
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return url;
    }

    /**
     * 文件流上传
     * @param inputStream
     * @param targetPath
     * @param fileName
     * @return
     */
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName) {
        String url = null;
        String key = buildKey(targetPath, fileName);
        String keyPath = getOssKey(key);
        try(InputStream in = inputStream;ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
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
            } catch (SdkClientException e) {
                log.error("set PublicRead failed:", e);
            }

            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, keyPath);
            String ossUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
            if (ossUrl.contains(FileConstant.HTTPS_PREFIX)) {
                url = ossUrl.replaceAll(FileConstant.HTTPS_DOMAIN_REGEX, StringUtil.EMPTY);
            }
            if (ossUrl.contains(FileConstant.HTTP_PREFIX)) {
                url = ossUrl.replaceAll(FileConstant.HTTP_DOMAIN_REGEX, StringUtil.EMPTY);
            }
            if (URL_TAKE_SIGN_NO == urlTakeSign) {
                url = url.split("\\?")[0];
            }
            return url;
        } catch (SdkClientException e) {
            log.error(e.getMessage());
        } catch (Exception e){
            log.error("upload OSS error！", e);
        }
        return StringUtils.EMPTY;
    }
    

}
