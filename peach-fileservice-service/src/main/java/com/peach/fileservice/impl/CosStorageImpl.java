package com.peach.fileservice.impl;

import cn.hutool.core.io.FileUtil;

import com.google.common.collect.Lists;
import com.peach.fileservice.AbstractFileStorageService;
import com.peach.fileservice.config.FileProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/2/28 16:33
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "cos")
public class CosStorageImpl extends AbstractFileStorageService {

    private final COSClient cosClient;

    private final String bucketName;

    public CosStorageImpl(FileProperties properties) {
        String accessKey = properties.getCos().getAccessKey();
        String secretKey = properties.getCos().getSecretKey();
        String region = properties.getCos().getRegion();
        COSCredentials crd = new BasicCOSCredentials(accessKey, secretKey);
        ClientConfig config = new ClientConfig();
        config.setRegion(new Region(region));
        config.setHttpProtocol(HttpProtocol.https);
        this.cosClient = new COSClient(crd,config);
        this.bucketName = properties.getCos().getBucketName();
        log.info("cos init success ...");
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
        return upLoadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes())){
            return upLoadInputStream(inputStream, targetPath, fileName);
        }catch (Exception e){
            log.error("upload file failed");
            return "";
        }
    }

    @Override
    public List<String> upload(File[] file, String targetPath) {
        List<String> urlList =  Lists.newArrayList();
        if (file == null) {
            return urlList;
        }
        for (File f : file) {
            String url = upload(f, targetPath,FileUtil.getName(f));
            urlList.add(url);
        }
        return urlList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        try (InputStream inputStream = FileUtil.getInputStream(file)) {
            return upLoadInputStream(inputStream, targetPath, fileName);
        }catch (Exception e){
            log.error("uplod file failed");
            return "";
        }
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        File file = null;
        try (InputStream inputStream = getInputStreamByKey(targetPath) ) {
            String downloadPath = (localPath.endsWith(File.separator) || localPath.endsWith(PATH_SEPARATOR)) ? localPath : localPath + File.separator;
            if (inputStream != null){
                file = FileUtil.writeFromStream(inputStream, new File(downloadPath +  fileName));
            }
        }catch (Exception ex){
            log.error("download file error！", ex);
        }
        if (file != null){
            return file.exists();
        }
        return false;
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        return null;
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        InputStream inputStream = null;
        String ossKey = key;
        try {
            ossKey = URLDecoder.decode(ossKey, "UTF-8");
            inputStream = cosClient.getObject(bucketName, getOssKey(ossKey)).getObjectContent();
            return inputStream;
        } catch (Exception e) {
            log.error("获取文件失败:[" + key + "]");
            ossKey = ossKey.replace(bucketName + "/", "");
            try {
                log.info("bucketName:[" + bucketName + "],replace key:[" + ossKey + "]");
                inputStream = cosClient.getObject(bucketName, ossKey).getObjectContent();
                return inputStream;
            } catch (Exception ex) {
                log.error("修改key 后获取文件失败:[" + ossKey + "] ", e);
            }
        }
        return null;
    }

    @Override
    public boolean delete(String key) {
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


    private String upLoadInputStream(InputStream inputStream, String targetPath, String fileName) {
        String url = null;
        String localPath = targetPath.endsWith(PATH_SEPARATOR) ? targetPath : targetPath + PATH_SEPARATOR;
        String key = localPath + fileName;
        try {
            //上传文件
            PutObjectResult result = cosClient.putObject(bucketName, getOssKey(key), inputStream, null);
            if (null != result) {
                // 设置URL过期时间为2年
                Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
                String ossUrl = cosClient.generatePresignedUrl(bucketName, getOssKey(key), expiration).toString();
                url = removeUrlHost(ossUrl);
            }
        } catch (Exception e) {
            log.error("upload OSS error！", e);
        }
        return url;
    }
}
