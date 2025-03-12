package com.peach.fileservice.impl.file;

import cn.hutool.core.io.FileUtil;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.impl.AbstractFileStorageService;
import com.peach.fileservice.config.FileProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 文件上传到本地实现
 * @CreateTime 2025/3/10 10:34
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "local")
public class LocalStorageImpl extends AbstractFileStorageService {

    private final String nginxProxy;

    private final String tempPath;

    private static final String REGEX = "^[^?]+";

    public LocalStorageImpl(FileProperties fileProperties) {
        this.nginxProxy = fileProperties.getNginxProxy();
        this.tempPath = fileProperties.getLocal().getTempPath();
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        FileUtil.copy(sourceDir, targetDir, true);
        return FileUtil.exist(targetDir);
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        FileUtil.copy(sourceDir, localDir, true);
        return FileUtil.exist(localDir);
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return uploadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try(InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            return uploadInputStream(inputStream, targetPath, fileName);
        }catch (IOException ex ){
            log.error("upload file failed");
            return null;
        }
    }

    @Override
    public List<String> upload(File[] file, String targetPath) {
        if (file == null || file.length == 0){
            return Collections.emptyList();
        }
        List<String> urlList = Lists.newArrayList();
        for (File f : file) {
            String url = uploadFile(f, targetPath, f.getName());
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
        if (StringUtil.isBlank(targetPath) || StringUtil.isBlank(localPath) || StringUtil.isBlank(fileName)){
            log.error("下载文件失败,下载参数错误");
            return false;
        }

        InputStream inputStream = null;
        try{
            targetPath = URLDecoder.decode(targetPath, PubCommonConst.UTF_8);
            targetPath = tempPath + PATH_SEPARATOR + targetPath;
            Pattern pattern = Pattern.compile("^[^?]+");
            Matcher matcher = pattern.matcher(targetPath);
            targetPath = matcher.find() ? matcher.group() : targetPath;
            inputStream = new FileInputStream(targetPath);
            if (inputStream == null){
                return false;
            }
            localPath = localPath.endsWith(PATH_SEPARATOR) ? localPath  : localPath + PATH_SEPARATOR;
            File file = FileUtil.writeFromStream(inputStream, localPath + fileName);
            return FileUtil.exist(file);
        }catch (Exception ex){
            log.error("download file failed"+ex.getMessage(),ex);
            return false;
        }finally {
            try {
                if (inputStream != null){
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("download file failed"+e.getMessage(),e);
            }
        }
    }

    /**
     * 判断目标路劲是否包含fileName 不包含+fileName  包含的话直接使用targetPath
     * @param targetPath （样例：/data/nfsdata/ 或者 /data/nfsdata）
     * @param fileName   (样例：test.yml)
     * @return
     */
    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        if (StringUtil.isBlank(targetPath) || StringUtil.isBlank(fileName)){
            log.error("params error");
            return null;
        }
        try {
            String key;
            String decodePath = URLDecoder.decode(targetPath, PubCommonConst.UTF_8);
            if (decodePath.contains(fileName)){
                key = decodePath;
            }else {
                decodePath = decodePath.endsWith(PATH_SEPARATOR) ? decodePath  : decodePath + PATH_SEPARATOR;
                key = decodePath + fileName;
            }
            return FileUtil.getInputStream(key);
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException"+e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        String finalPath = key.contains("?") ? key.substring(0, key.indexOf("?")) : key;
        try {
            String decodePath = URLDecoder.decode(finalPath, PubCommonConst.UTF_8);
            if (!FileUtil.exist(decodePath)){
                return null;
            }
            return FileUtil.getInputStream(decodePath);
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException"+e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(String key) {
        boolean hasIllegalChar = isHasIllegalChar(key);
        if (hasIllegalChar){
            return hasIllegalChar;
        }
        if (key.contains("?")){
            key = key.substring(0, key.indexOf("?"));
        }
        key = removeUrlHost(key);
        try {
            key = URLDecoder.decode(key, PubCommonConst.UTF_8);
            FileUtil.del(key);
            return true;
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException"+e.getMessage(),e);
            return false;
        }
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        return false;
    }

    @Override
    public String getUrlByKey(String key) {
        return getCompleteUrlByKey(key,true);
    }

    @Override
    public String getPathByKey(String key) {
        return getCompleteUrlByKey(key,false);
    }

    @Override
    public void setPublicReadAcl(String path) {
        log.info("本地文件系统不支持设置权限");
    }


    /**
     * 通过key获取到完整的url
     * @param key
     * @param isUrl
     * @return String 完整的url
     */
    private String getCompleteUrlByKey(String key,boolean isUrl) {
        if (!FileUtil.exist(key)) {
            log.error("key is null");
            return null;
        }
        key = decodeAndGetAbsolutePath(key);
        String[] pathArr = key.split(PATH_SEPARATOR);
        StringBuilder keyPath = new StringBuilder();
        for (String path : pathArr) {
            if (jodd.util.StringUtil.isNotBlank(path)) {
                try {
                    path = java.net.URLEncoder.encode(path, PubCommonConst.UTF_8).replaceAll("\\+", "%20");
                } catch (UnsupportedEncodingException e) {
                    log.error("文件路径转码失败!", e);
                }
                keyPath.append(PATH_SEPARATOR);
                keyPath.append(path);
            }
        }
        return (isUrl ? nginxProxy : "") + keyPath + "?timestamp=" + System.currentTimeMillis();
    }


    /**
     * 解码key，并获取绝对路径
     * @param key
     * @return
     */
    public static String decodeAndGetAbsolutePath(String key) {
        try {
            if (StringUtils.isBlank(key)) {
                return null;
            }
            log.info("decode 之前 key:" + key);
            key = URLDecoder.decode(key, PubCommonConst.UTF_8);
            log.info("decode 之后 key:" + key);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        boolean isAbsolutePath = FileUtil.isAbsolutePath(key);
        if (!isAbsolutePath) {
            key = "/" + key;
        }
        log.info("absolutePath key:" + key);
        return key;
    }



    /**
     * 定义本地上传的逻辑，其他存储方式需要重写此方法
     * @param inputStream
     * @param targetPath
     * @param fileName
     * @return
     */
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName) {
        // 1、上传文件
        String localPath = targetPath.endsWith(PATH_SEPARATOR) ?  targetPath : targetPath + PATH_SEPARATOR ;
        String key = tempPath + PATH_SEPARATOR + localPath + fileName;

        try {
            File file = retryer.call(() -> {
                log.info("尝试写入文件: {}", key);
                return FileUtil.writeFromStream(inputStream, key);
            });
        } catch (Exception e) {
            log.error("文件上传失败，重试 3 次仍然失败: {}", e.getMessage(), e);
            return null;
        }

        try {
            return localPath.replace(File.separator, PATH_SEPARATOR) + URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20") + "?timestamp=" + System.currentTimeMillis();
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException"+e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }


}
