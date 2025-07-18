package com.peach.fileservice.impl.file;

import cn.hutool.core.io.FileUtil;
import com.google.common.collect.Lists;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.config.FileProperties;
import com.peach.fileservice.impl.AbstractFileStorageService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

    private final String nginxProxy;

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
        this.nginxProxy = properties.getNginxProxy();
        log.info("cos init success ...");
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        try {
            sourceDir = handlerKeyPrefix(sourceDir);
            targetDir = handlerKeyPrefix(targetDir);

            ObjectListing objectListing;
            String nextMarker = null;

            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(sourceDir)
                        .withMarker(nextMarker);

                objectListing = cosClient.listObjects(listObjectsRequest);

                for (COSObjectSummary summary : objectListing.getObjectSummaries()) {
                    String sourceKey = summary.getKey();
                    String relativeKey = sourceKey.substring(sourceDir.length());
                    String targetKey = targetDir + relativeKey;
                    cosClient.copyObject(bucketName, sourceKey, bucketName, targetKey);
                }

                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
            return true;
        } catch (Exception e) {
            log.error("copyDir error! sourceDir={}, targetDir={}", sourceDir, targetDir, e);
            return Boolean.FALSE;
        }
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        try {
            sourceDir = handlerKeyPrefix(sourceDir);
            String nextMarker = null;
            ObjectListing objectListing;

            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(sourceDir)
                        .withMarker(nextMarker);

                objectListing = cosClient.listObjects(listObjectsRequest);

                for (COSObjectSummary  summary : objectListing.getObjectSummaries()) {
                    String key = summary.getKey();
                    String relativePath = key.substring(sourceDir.length());
                    InputStream inputStream = cosClient.getObject(bucketName, key).getObjectContent();
                    File localFile = new File(localDir, relativePath);
                    FileUtil.writeFromStream(inputStream, localFile);
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
            return true;
        } catch (Exception e) {
            log.error("downDir error! sourceDir={}, localDir={}", sourceDir, localDir, e);
            return Boolean.FALSE;
        }
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return uploadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes())){
            return uploadInputStream(inputStream, targetPath, fileName);
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
            String url = uploadFile(f, targetPath,FileUtil.getName(f));
            urlList.add(url);
        }
        return urlList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        return uploadFile(file, targetPath, fileName);
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        File file = null;
        try (InputStream inputStream = getInputStreamByKey(targetPath) ) {
            String downloadPath = normalizeDirectory(localPath);
            if (inputStream != null){
                file = FileUtil.writeFromStream(inputStream, new File(buildKey(downloadPath , fileName)));
            }
        }catch (Exception ex){
            log.error("download file filed"+ex.getMessage(), ex);
        }
        return FileUtil.exist(file);
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        InputStream inputStream = null;
        try {
            String key;
            String decodePath = URLDecoder.decode(targetPath, PubCommonConst.UTF_8);
            if (decodePath.contains(fileName)){
                key = decodePath;
            }else {
                key = buildKey(decodePath, fileName);
            }
            inputStream = cosClient.getObject(bucketName, getOssKey(key)).getObjectContent();
        } catch (UnsupportedEncodingException e) {
            log.error("getInputStream UnsupportedEncoding filed"+e.getMessage(),e);
        }
        return inputStream;
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        InputStream inputStream = null;
        String ossKey = key;
        try {
            ossKey = URLDecoder.decode(ossKey, PubCommonConst.UTF_8);
            inputStream = cosClient.getObject(bucketName, getOssKey(ossKey)).getObjectContent();
            return inputStream;
        } catch (Exception e) {
            log.error("getInputStreamByKey field:[{}]",key);
            ossKey = ossKey.replace(bucketName + "/", "");
            try {
                log.info("bucketName:[｛｝],replace key:[｛｝]",bucketName,ossKey);
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
        // 校验 key 是否含非法字符，不符合要求则直接返回 false
        if (isHasIllegalChar(key)) {
            log.error("delete file failed, [{}] is illegal,can't be deleted", key);
            return Boolean.FALSE;
        }
        boolean flag = true;
        try {
            key = removeUrlHost(key);
            String nextMarker = null;
            ObjectListing objectListing;

            do {
                // 查询 keys
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                        .withBucketName(bucketName)
                        .withPrefix(handlerKeyPrefix(key))
                        .withMarker(nextMarker);
                objectListing = cosClient.listObjects(listObjectsRequest);

                // 构建删除列表
                List<DeleteObjectsRequest.KeyVersion> keys = objectListing.getObjectSummaries()
                        .stream()
                        .map(s -> new DeleteObjectsRequest.KeyVersion(s.getKey()))
                        .collect(Collectors.toList());

                if (!keys.isEmpty()) {
                    List<String> keyList = keys.stream().map(DeleteObjectsRequest.KeyVersion::getKey).collect(Collectors.toList());
                    try {
                        log.info("即将删除 {} 个对象: {}", keys.size(), keyList);
                        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
                                .withKeys(keys);
                        cosClient.deleteObjects(deleteObjectsRequest);
                        log.info("成功删除 {} 个对象", keys.size());
                    } catch (Exception e) {
                        flag = Boolean.FALSE;  // 只要删除失败，最终返回 false
                        log.error("删除对象失败: {}", keyList, e);
                    }
                }

                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());

            log.info("目录 [{}] 及其所有文件已删除完成", key);
        } catch (Exception e) {
            flag = Boolean.FALSE;
            log.error("CosDeleteObject error！key: {}", key, e);
        }
        return flag;
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        try {
            currentPath = handlerKeyPrefix(currentPath);
            targetPath = handlerKeyPrefix(targetPath);
            cosClient.copyObject(bucketName, currentPath, bucketName, targetPath);
            return true;
        } catch (Exception e) {
            log.error("copyFile error! currentPath={}, targetPath={}", currentPath, targetPath, e);
            return Boolean.FALSE;
        }
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
            String key = handlerKeyPrefix(path);
            key = key.endsWith("/") ? key.substring(0, key.length() - 1) : key; // 去掉结尾/，避免设置目录无效
            cosClient.setObjectAcl(bucketName, getOssKey(key), com.qcloud.cos.model.CannedAccessControlList.PublicRead);
            log.info("已设置对象 [{}] 为 PublicRead", key);
        } catch (Exception e) {
            log.error("设置对象权限失败 path: {}", path, e);
        }
    }

    protected String getOrgUrlByKey(String key, boolean isUrl) {
        try {
            key = handlerKeyPrefix(URLDecoder.decode(key, PubCommonConst.UTF_8));
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION); // 可配置默认值
            String ossUrl = cosClient.generatePresignedUrl(bucketName, key, expiration).toString();
            String url = StringUtil.EMPTY;
            if (ossUrl.contains(FileConstant.HTTPS_PREFIX)) {
                url = ossUrl.replaceAll(FileConstant.HTTPS_DOMAIN_REGEX, isUrl ? nginxProxy : StringUtil.EMPTY);
            }
            if (ossUrl.contains(FileConstant.HTTP_PREFIX)) {
                url = ossUrl.replaceAll(FileConstant.HTTP_DOMAIN_REGEX, isUrl ? nginxProxy : StringUtil.EMPTY);
            }
            return url;
        } catch (Exception e) {
            log.error("getUrlByKey error! key={}", key, e);
            return StringUtil.EMPTY;
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
        String url = StringUtil.EMPTY;
        String key = buildKey(targetPath, fileName);
        try {
            //上传文件
            PutObjectResult result = cosClient.putObject(bucketName, getOssKey(key), inputStream, null);
            if (null != result) {
                // 设置URL过期时间为2年
                Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
                String ossUrl = cosClient.generatePresignedUrl(bucketName, getOssKey(key), expiration).toString();
                url = removeUrlHost(ossUrl);
                setPublicReadAcl(url);
            }
        } catch (Exception e) {
            log.error("upload file to cos failed！"+e.getMessage(), e);
        }
        return url;
    }
}
