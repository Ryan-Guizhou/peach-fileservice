package com.peach.fileservice.impl;

import cn.hutool.core.io.FileUtil;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectResult;
import com.google.common.collect.Lists;
import com.peach.fileservice.AbstractFileStorageService;
import com.peach.fileservice.config.FileProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/9 15:00
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "oss")
public class OssStorageImpl extends AbstractFileStorageService {

    private final OSSClient ossClient;

    private final String bucketName;

    public OssStorageImpl(FileProperties properties) {
        FileProperties.OssConfig ossConfig = properties.getOss();
        this.ossClient = new OSSClient(ossConfig.getEndpoint(), ossConfig.getAccessKey(), ossConfig.getSecretKey());
        this.bucketName = ossConfig.getBucketName();
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
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))){
            upLoadInputStream(inputStream,targetPath,fileName);
        }catch (Exception ex){
            log.error("upload oss error");
        }
        return null;
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        List<String> urlList = Lists.newArrayList();
        if (files == null){
            return urlList;
        }
        for (File file : files) {
            String fileName = FileUtil.getName(file);
            String key = targetPath + fileName;
            urlList.add(upLoadFile(file, key, fileName));
        }
        return urlList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        return upLoadFile(file,targetPath,fileName);
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
        //真实存储路径 前缀
        try {
            key = URLDecoder.decode(key, "UTF-8");
            inputStream = ossClient.getObject(bucketName, getOssKey(key)).getObjectContent();
        } catch (Exception e) {
            log.error("getInputStream error！", e);
        }
        return inputStream;
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

    private String upLoadFile(File file, String targetPath, String fileName) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return upLoadInputStream(fileInputStream, targetPath, fileName);
        } catch (Exception e) {
            log.error("file save error ", e);
        }
        return null;
    }

    private String upLoadInputStream(InputStream inputStream, String targetPath, String fileName) {
        String url = null;
        String localPath = targetPath.endsWith(PATH_SEPARATOR) ? targetPath : targetPath + PATH_SEPARATOR;
        String key = localPath + fileName;
        try {
            //上传文件
            PutObjectResult result = ossClient.putObject(bucketName, getOssKey(key), inputStream);
            if (null != result) {
                // 设置URL过期时间为2年
                Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
                String ossUrl = ossClient.generatePresignedUrl(bucketName, getOssKey(key), expiration).toString();
                url = removeUrlHost(ossUrl);
            }
        } catch (Exception e) {
            log.error("upload OSS error！", e);
        }
        return url;
    }

}
