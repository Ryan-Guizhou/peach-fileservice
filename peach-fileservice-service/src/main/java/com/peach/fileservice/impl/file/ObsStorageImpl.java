package com.peach.fileservice.impl.file;

import cn.hutool.core.io.FileUtil;
import com.google.common.collect.Lists;
import com.obs.services.ObsClient;
import com.obs.services.model.*;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.config.FileProperties;
import com.peach.fileservice.impl.AbstractFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "obs")
public class ObsStorageImpl extends AbstractFileStorageService {

    private final String accessKey;

    private final String secretKey;

    private final String endpoint;

    private final String bucketName;
    
    private final String nginxProxy;

    private final ObsClient obsClient;


    public ObsStorageImpl(final FileProperties fileProperties) {
        FileProperties.ObsConfig obsConfig = fileProperties.getObs();
        this.endpoint = obsConfig.getEndpoint();
        this.accessKey = obsConfig.getAccessKey();
        this.secretKey = obsConfig.getSecretKey();
        this.bucketName = obsConfig.getBucketName();
        this.nginxProxy = fileProperties.getNginxProxy();
        obsClient = new ObsClient(accessKey, secretKey, endpoint);
        log.info("ObsClient initialized successfully");
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        boolean flag = Boolean.TRUE;
        String sourceDirKey = handlerKeyPrefix(sourceDir);
        String targetDirKey = handlerKeyPrefix(targetDir);

        try {
            // 删除目录及目录下的所有文件。
            String nextMarker = null;
            ObjectListing objectListing;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setPrefix(sourceDirKey);
                listObjectsRequest.setMarker(nextMarker);
                objectListing = obsClient.listObjects(listObjectsRequest);
                for (ObsObject objectSummary : objectListing.getObjects()) {
                    String sourceKey = objectSummary.getObjectKey();
                    try {
                        String targetKey = sourceKey.replace(sourceDirKey, targetDirKey);
                        CopyObjectRequest copyObjRequest = new CopyObjectRequest(bucketName, sourceKey, bucketName, targetKey);
                        obsClient.copyObject(copyObjRequest);
                    } catch (Exception e) {
                        flag = Boolean.FALSE;
                        log.error("copy object field" + e.getMessage(),e);
                    }
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (Exception e) {
            flag = Boolean.FALSE;
            log.error("copy object error"+e.getMessage(), e);
        }
        return flag;
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        try {
            // 删除目录及目录下的所有文件。
            String nextMarker = null;
            ObjectListing objectListing;
            //查询keys
            String sourceDirKey = handlerKeyPrefix(sourceDir);
            String localDirKey = handlerKeyPrefix(localDir);

            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setPrefix(sourceDirKey);
                listObjectsRequest.setMarker(nextMarker);
                objectListing = obsClient.listObjects(listObjectsRequest);
                for (ObsObject objectSummary : objectListing.getObjects()) {
                    // 下载文件到本地目录。
                    String sourceKey = objectSummary.getObjectKey();
                    String localKey = sourceKey.replace(sourceDirKey, localDirKey);
                    try (InputStream inputStream = obsClient.getObject(bucketName, sourceKey).getObjectContent()) {
                        if (inputStream != null) {
                            FileUtil.writeFromStream(inputStream, localKey);
                        }
                    } catch (Exception e) {
                        log.error(sourceKey + " download error！" + e.getMessage());
                    }
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (Exception e) {
            log.error("download error！", e);
        }
        return FileUtil.exist(localDir);
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return uploadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            return uploadInputStream(inputStream, targetPath, fileName);
        } catch (IOException e) {
            log.error("upload OSS error！", e);
            return null;
        }
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        if (files == null || files.length == 0) {
            log.error("file is null or empty");
            return Lists.newArrayList();
        }
        List<String> urlList = new ArrayList<>();
        for (File file : files) {
            String url = uploadFile(file, targetPath, FileUtil.getName(file));
            urlList.add(url);
        }
        return urlList;

    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        return "";
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        try (InputStream inputStream = this.getInputStreamByKey(targetPath)) {
            String downPath = normalizeDirectory(localPath);
            if (inputStream != null) {
                FileUtil.writeFromStream(inputStream, buildKey(downPath , fileName));
            }
            return FileUtil.exist(buildKey(downPath , fileName));
        } catch (Exception e) {
            log.error("obsStorageImpl download file error！", e);
            return Boolean.FALSE;
        }
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        InputStream inputStream = null;
        //真实存储路径 前缀
        try {
            String decodePath = URLDecoder.decode(targetPath, PubCommonConst.UTF_8);
            String key = decodePath.contains(fileName) ? decodePath : buildKey(decodePath,fileName);
            return obsClient.getObject(bucketName, getOssKey(key)).getObjectContent();
        } catch (Exception e) {
            log.error("obsStorageImpl getInputStream error！", e);
            return null;
        }
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        try {
            return obsClient.getObject(bucketName, getOssKey(key)).getObjectContent();
        } catch (Exception e) {
            log.error("obsStorageImpl getInputStreamByKey failed"+e.getMessage(), e);
            return null;
        }

    }

    @Override
    public boolean delete(String key) {
        if (isHasIllegalChar(key)) {
            log.error("delete file failed, [{}] is illegal,can't be deleted", key);
            return Boolean.FALSE;
        }
        boolean isDeleted = Boolean.TRUE;
        try  {
            key = removeUrlHost(key);
            // 删除目录及目录下的所有文件。
            String nextMarker = null;
            ObjectListing objectListing;
            do {
                //查询keys
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setPrefix(handlerKeyPrefix(key));
                listObjectsRequest.setMarker(nextMarker);
                objectListing = obsClient.listObjects(listObjectsRequest);
                //遍历删除
                if (!objectListing.getObjects().isEmpty()) {
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
                    for (ObsObject s : objectListing.getObjects()) {
                        deleteObjectsRequest.addKeyAndVersion(s.getObjectKey());
                    }
                    deleteObjectsRequest.setEncodingType("url");
                    obsClient.deleteObjects(deleteObjectsRequest);
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (Exception e) {
            isDeleted = Boolean.FALSE;
            log.error("OssDeleteObject failed"+e.getMessage(), e);
        }
        return isDeleted;
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
            obsClient.setObjectAcl(bucketName, getOssKey(path), AccessControlList.REST_CANNED_PUBLIC_READ);
        } catch (Exception e) {
            log.error("osbStorageImpl setPublicReadAcl field"+e.getMessage(), e);
        }
    }

    protected String getOrgUrlByKey(String key, boolean isUrl) {
        
        String keyPath = getOssKey(key);
        String url = StringUtil.EMPTY;
        try {
            boolean flag = obsClient.doesObjectExist(bucketName, keyPath);
            if (!flag) {
                log.error("文件不存在!");
                return url;
            }
            long expiration = System.currentTimeMillis() + EXPIRATION;
            TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET, expiration);
            //设置桶名,一般都是写在配置里，这里直接赋值即可
            request.setBucketName(bucketName);
            //这里相当于设置你上传到obs的文件路
            request.setObjectKey(keyPath);
            TemporarySignatureResponse response = obsClient.createTemporarySignature(request);
            String ossUrl = response.getSignedUrl();
            if (ossUrl.contains(FileConstant.HTTPS_DOMAIN_REGEX)) {
                url = ossUrl.replaceAll(FileConstant.HTTPS_DOMAIN_REGEX, isUrl ? nginxProxy : StringUtil.EMPTY);
            }
            if (ossUrl.contains(FileConstant.HTTP_DOMAIN_REGEX)) {
                url = ossUrl.replaceAll(FileConstant.HTTP_DOMAIN_REGEX, isUrl ?  nginxProxy : StringUtil.EMPTY);
            }
        } catch (Exception e) {
            log.error("obsStorageImpl getUrlByKey failed"+e.getMessage(), e);
        }
        return url;
    }
    
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName) {
        String url = StringUtil.EMPTY;
        String key = buildKey(targetPath, fileName);
        try {
            //上传文件
            PutObjectResult result = obsClient.putObject(bucketName, getOssKey(key), inputStream);
            if (result == null) {
                log.error("upload result is null");
                return url;
            }
            obsClient.setObjectAcl(bucketName, getOssKey(key), AccessControlList.REST_CANNED_PUBLIC_READ);
            // 设置URL过期时间为2年
            long expiration = System.currentTimeMillis() + EXPIRATION;
            TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET, expiration);
            //设置桶名,一般都是写在配置里，这里直接赋值即可
            request.setBucketName(bucketName);
            //这里相当于设置你上传到obs的文件路
            request.setObjectKey(getOssKey(key));
            TemporarySignatureResponse response = obsClient.createTemporarySignature(request);
            String ossUrl = response.getSignedUrl();
            url = removeUrlHost(ossUrl);
        } catch (Exception e) {
            log.error("upload obs error！", e);
        }
        return url;
    }
}
