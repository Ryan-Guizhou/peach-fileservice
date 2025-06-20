package com.peach.fileservice.impl.file;

import cn.hutool.core.io.FileUtil;
import com.google.common.collect.Lists;
import com.peach.common.constant.PubCommonConst;
import com.peach.fileservice.config.FileProperties;
import com.peach.fileservice.impl.AbstractFileStorageService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.PutObjectResult;
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
            String downloadPath = (localPath.endsWith(File.separator) || localPath.endsWith(PATH_SEPARATOR)) ? localPath : localPath + File.separator;
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
            String key;
            String decodePath = URLDecoder.decode(targetPath, PubCommonConst.UTF_8);
            if (decodePath.contains(fileName)){
                key = decodePath;
            }else {
                decodePath = decodePath.endsWith(PATH_SEPARATOR) ? decodePath : decodePath + PATH_SEPARATOR;
                key = decodePath + fileName;
            }
            inputStream = cosClient.getObject(bucketName, getOssKey(key)).getObjectContent();
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException"+e.getMessage(),e);
        }
        return inputStream;
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
        // 校验 key 是否含非法字符，不符合要求则直接返回 false
        if (isHasIllegalChar(key)) {
            log.warn("非法 key，禁止删除: {}", key);
            return false;
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
                        flag = false;  // 只要删除失败，最终返回 false
                        log.error("删除对象失败: {}", keyList, e);
                    }
                }

                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());

            log.info("目录 [{}] 及其所有文件已删除完成", key);
        } catch (Exception e) {
            flag = false;
            log.error("CosDeleteObject error！key: {}", key, e);
        }
        return flag;
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

    /**
     * 上传文件
     * @param file
     * @param targetPath
     * @param fileName
     * @return
     */
    protected String uploadFile(File file,String targetPath,String fileName){
        try (InputStream inputStream = FileUtil.getInputStream(file)){
            return uploadInputStream(inputStream,targetPath,fileName);
        }catch (Exception ex){
            log.error("upload file to cos failed");
            return null;
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
            PutObjectResult result = cosClient.putObject(bucketName, getOssKey(key), inputStream, null);
            if (null != result) {
                // 设置URL过期时间为2年
                Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
                String ossUrl = cosClient.generatePresignedUrl(bucketName, getOssKey(key), expiration).toString();
                url = removeUrlHost(ossUrl);
            }
        } catch (Exception e) {
            log.error("upload file to cos failed！"+e.getMessage(), e);
        }
        return url;
    }
}
