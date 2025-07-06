package com.peach.fileservice.impl.file;

import cn.hutool.core.io.FileUtil;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import com.google.common.collect.Lists;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.impl.AbstractFileStorageService;
import com.peach.fileservice.config.FileProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    private final String nginxProxy;

    public OssStorageImpl(FileProperties properties) {
        FileProperties.OssConfig ossConfig = properties.getOss();
        this.ossClient = new OSSClient(ossConfig.getEndpoint(), ossConfig.getAccessKey(), ossConfig.getSecretKey());
        this.bucketName = ossConfig.getBucketName();
        this.nginxProxy = properties.getNginxProxy();
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
        return uploadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))){
            uploadInputStream(inputStream,targetPath,fileName);
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
            urlList.add(uploadFile(file, key, fileName));
        }
        return urlList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        return uploadFile(file,targetPath,fileName);
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        File file = null;
        try (InputStream inputStream = getInputStreamByKey(targetPath) ) {
            String downloadPath = normalizeDirectory(localPath);
            if (inputStream != null){
               file = FileUtil.writeFromStream(inputStream, new File(downloadPath +  fileName));
            }
        }catch (Exception ex){
            log.error("download file error！", ex);
        }
        return FileUtil.exist(file);
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        InputStream inputStream = null;
        try {
            String key ;
            String decodePath = URLDecoder.decode(targetPath, PubCommonConst.UTF_8);
            if (decodePath.contains(fileName)){
                key = decodePath;
            }else {
                key = buildKey(decodePath, fileName);
            }
            inputStream = ossClient.getObject(bucketName, getOssKey(key)).getObjectContent();
        } catch (UnsupportedEncodingException e) {
           log.error("ossStorageImpl unsupportedEncoding field "+e.getMessage(),e);
        }
        return inputStream;
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        InputStream inputStream = null;
        //真实存储路径 前缀
        try {
            key = URLDecoder.decode(key, PubCommonConst.UTF_8);
            inputStream = ossClient.getObject(bucketName, getOssKey(key)).getObjectContent();
        } catch (Exception e) {
            log.error("ossStorageImpl getInputStream field！"+e.getMessage(), e);
        }
        return inputStream;
    }

    @Override
    public boolean delete(String key) {

        // 校验删除文件的key，如果不符合要求 直接返回
        boolean hasIllegalChar = isHasIllegalChar(key);
        if (hasIllegalChar){
            log.error("delete file failed, [{}] is illegal,can't be deleted", key);
            return Boolean.FALSE;
        }

        boolean flag = Boolean.TRUE;
        try {
            key = removeUrlHost(key);
            // 删除目录及目录下的所有文件。
            String nextMarker = null;
            ObjectListing objectListing;
            do {
                //查询keys
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                        .withPrefix(handlerKeyPrefix(key))
                        .withMarker(nextMarker);
                objectListing = ossClient.listObjects(listObjectsRequest);

                //遍历删除
                if (!objectListing.getObjectSummaries().isEmpty()) {
                    List<String> keys = new ArrayList<>();
                    for (OSSObjectSummary s : objectListing.getObjectSummaries()) {
                        keys.add(s.getKey());
                    }
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName).withKeys(keys).withEncodingType("url");
                    ossClient.deleteObjects(deleteObjectsRequest);
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (Exception e) {
            flag = false;
            log.error("OssDeleteObject error！", e);
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
            path = URLDecoder.decode(path,PubCommonConst.UTF_8);
        }catch (Exception ex){
            log.error("ossStorageImpl unsupportedEncoding field "+ex.getMessage(),ex);
        }
        ossClient.setObjectAcl(bucketName,getOssKey(path),CannedAccessControlList.PublicRead);
    }

    protected String getOrgUrlByKey(String key, boolean isUrl) {
        try {
            key = URLDecoder.decode(key,PubCommonConst.UTF_8);
        }catch (Exception ex){
            log.error("ossStorageImpl unsupportedEncoding field "+ex.getMessage(),ex);
            throw new RuntimeException("ossStorageImpl unsupportedEncoding field "+ex.getMessage(),ex);
        }
        String keyPath = getOssKey(key);
        String url = StringUtil.EMPTY;
        try {
            boolean isExist = ossClient.doesObjectExist(bucketName, keyPath);
            if (!isExist){
                log.error("ossStorageImpl doesObjectExist fail");
                return url;
            }
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            String ossUrl = ossClient.generatePresignedUrl(bucketName,keyPath,expiration).toString();
            if (ossUrl.contains(FileConstant.HTTPS_PREFIX)) {
                url = ossUrl.replaceAll(FileConstant.HTTPS_DOMAIN_REGEX, isUrl ? nginxProxy : StringUtil.EMPTY);
            }
            if (ossUrl.contains(FileConstant.HTTP_PREFIX)) {
                url = ossUrl.replaceAll(FileConstant.HTTP_DOMAIN_REGEX, isUrl ? nginxProxy : StringUtil.EMPTY);
            }
            return url;
        }catch (Exception ex){
            log.error("ossStorageImpl unsupportedEncoding field "+ex.getMessage(),ex);
            return url;
        }
    }

    /**
     * 上传文件
     * @param inputStream
     * @param targetPath
     * @param fileName
     * @return
     */
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName) {
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
