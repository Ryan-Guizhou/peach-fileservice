package com.peach.fileservice.impl.file;

import cn.hutool.core.io.FileUtil;
import com.google.common.collect.Lists;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.common.enums.NasEnum;
import com.peach.fileservice.common.util.FileUtils;
import com.peach.fileservice.common.util.NasUtil;
import com.peach.fileservice.config.FileProperties;
import com.peach.fileservice.impl.AbstractFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
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
@ConditionalOnProperty(prefix = "file-storage", name = "type", havingValue = "nas")
public class NasStorageImpl extends AbstractFileStorageService {

    /**
     * 用于区分是本地NAS 还是 远程NAS
     */
    private final String type;

    private final String nginxProxy;


    public NasStorageImpl(FileProperties fileProperties) {
        FileProperties.NasConfig nasConfig = fileProperties.getNas();
        this.type = nasConfig.getType();
        this.nginxProxy = fileProperties.getNginxProxy();
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        if(NasEnum.REMOTE.name().equals(type)) {
            NasUtil.copyDir(sourceDir, targetDir);
        }
        // 拷贝文件 默认覆盖
        FileUtil.copy(sourceDir,targetDir,Boolean.TRUE);
        return FileUtil.exist(targetDir);
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        if(NasEnum.REMOTE.name().equals(type)) {
            NasUtil.downDir(sourceDir, localDir);
            return NasUtil.exist(localDir);
        }else {
            // 拷贝文件 默认覆盖
            FileUtil.copy(sourceDir,localDir,Boolean.TRUE);
        }
        return FileUtil.exist(localDir);
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        if (NasEnum.REMOTE.name().equals(type)) {
            return uploadInputStream(inputStream,targetPath,fileName);
        }else {
            return uploadLoclInputStream(inputStream,targetPath,fileName);
        }
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try(InputStream inputStream = new ByteArrayInputStream(content.getBytes())) {
            if(NasEnum.REMOTE.name().equals(type)) {
                return uploadInputStream(inputStream,targetPath,fileName);
            }else {
                return uploadLoclInputStream(inputStream,targetPath,fileName);
            }
        }catch (Exception e) {
            log.error("nasStorage upload field"+e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        if (files == null || files.length == 0){
            return org.apache.commons.compress.utils.Lists.newArrayList();
        }
        List<String> urlList = Lists.newArrayList();
        for (File file : files) {
            String url;
            if(NasEnum.REMOTE.name().equals(type)) {
                url = uploadFile(file,targetPath,FileUtil.getName(file));
            }else {
                url = uploadLoclFile(file,targetPath,FileUtil.getName(file));
            }
            urlList.add(url);
        }
        return urlList;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        if (NasEnum.REMOTE.name().equals(type)) {
            return uploadFile(file,targetPath,fileName);
        }else {
            return uploadLoclFile(file,targetPath,fileName);
        }
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        String localFilePath = buildKey(localPath,fileName);
        if(NasEnum.REMOTE.name().equals(type)) {
            NasUtil.downDir(targetPath, localFilePath);
        }else {
            FileUtil.copy(targetPath,localFilePath,Boolean.TRUE);
        }
        return FileUtil.exist(localFilePath);
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        try {
            String key = buildKey(targetPath,fileName);
            key = handlerKeyPrefix(key);
            key = URLDecoder.decode(key,PubCommonConst.UTF_8);
            if (NasEnum.REMOTE.name().equals(type)) {
                return NasUtil.getInputStream(key);
            }else {
                return Files.newInputStream(Paths.get(key));
            }
        }catch (Exception e) {
            log.error("nasStorage getInputStream error"+e.getMessage(), e);
            return null;
        }
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        try {
            key = handlerKeyPrefix(key);
            key = URLDecoder.decode(key,PubCommonConst.UTF_8);
            if (NasEnum.REMOTE.name().equals(type)) {
                return NasUtil.getInputStream(key);
            }else {
                return Files.newInputStream(Paths.get(key));
            }
        }catch (Exception e) {
            log.error("nasStorage getInputStream error"+e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        if (isHasIllegalChar(key)){
            log.error("nasStorage delete illegal char:[{}]",key);
            return Boolean.FALSE;
        }
        key = handlerKeyPrefix(key);
        key = removeUrlHost(key);
        try {
            key = URLDecoder.decode(key,PubCommonConst.UTF_8);
        }catch (UnsupportedEncodingException e) {
            log.error("nasStorage UnsupportedEncoding field"+e.getMessage(),e);
            return Boolean.FALSE;
        }
        if(NasEnum.REMOTE.name().equals(type)) {
            return NasUtil.delete(key);
        }
        return FileUtil.del(key);
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        if(NasEnum.REMOTE.name().equals(type)) {
            copyDir(currentPath,targetPath);
        }else {
            FileUtil.copy(currentPath,targetPath,Boolean.TRUE);
        }
        return FileUtil.exist(targetPath);
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
        log.info("nasStorageImpl don't need setPublicReadAcl");
    }

    /**
     * 通过键获取原始URL或路径, 默认返回本地的路径，Nas实现
     * @param key 文件键 / File key
     * @param isUrl 是否返回URL或者路径
     * @return URL 或者路径 / URL or Path
     */
    protected String getOrgUrlByKey(String key,boolean isUrl) {
        key = handlerKeyPrefix(key);
        key = FileUtils.decodeAndGetAbsolutePath(key);
        boolean isExist = NasEnum.REMOTE.name().equals(type) ? NasUtil.exist(key) : FileUtil.exist(key);
        if(!isExist) {
            log.error("file does not exist");
            return null;
        }
        String[] parts = key.split(FileConstant.PATH_SEPARATOR);
        StringBuffer keyPath = new StringBuffer();
        for (String path : parts) {
            if(StringUtil.isBlank(path)) {
                continue;
            }
            try {
                path = URLEncoder.encode(path,PubCommonConst.UTF_8).replaceAll("\\+", "%20");
            }catch (UnsupportedEncodingException e) {
                log.error("nasStorage encoding field"+e.getMessage(), e);
            }
            keyPath.append(FileConstant.PATH_SEPARATOR);
            keyPath.append(path);
        }
        return (isUrl ? nginxProxy : "") + new String(keyPath) + "?timestamp" +System.currentTimeMillis();
    }

    /**
     * 定义本地上传的逻辑，其他存储方式需要重写此方法
     * @param inputStream
     * @param targetPath
     * @param fileName
     * @return
     */
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName) {
        String url = StringUtil.EMPTY;
        String localPath = normalizeDirectory(targetPath);
        boolean isSuccess = NasUtil.upLoadInputStream(inputStream, targetPath, fileName);
        if (isSuccess){
            log.info("nasStorageImpl upload success,uploadPath:[{}]",targetPath + FileConstant.PATH_SEPARATOR + fileName);
            try {
                url = localPath.replace(File.separator, FileConstant.PATH_SEPARATOR) + URLEncoder.encode(fileName, PubCommonConst.UTF_8).replaceAll("\\+", "%20") + "?timestamp=" + System.currentTimeMillis();
                log.info("nasStorageImpl upload success,url:[{}]",url);
            }catch (UnsupportedEncodingException e){
                log.error("URL UnsupportedEncodingException"+e.getMessage(),e);
            }
        }else {
            log.error("nasStorageImpl upload fail,uploadPath:[{}]",localPath);
        }
        return url;
    }

    /**
     * 定义本地上传的逻辑，其他存储方式需要重写此方法
     * @param file
     * @param targetPath
     * @param fileName
     * @return
     */
    private String uploadLoclFile(File file, String targetPath, String fileName) {
        try (InputStream inputStream = FileUtil.getInputStream(file)){
            return uploadInputStream(inputStream, targetPath, fileName);
        }catch (Exception ex){
            log.error("upload file failed");
            return null;
        }
    }

    /**
     * 定义本地上传的逻辑，其他存储方式需要重写此方法
     * @param inputStream
     * @param targetPath
     * @param fileName
     * @return
     */
    private String uploadLoclInputStream(InputStream inputStream, String targetPath, String fileName) {
        return super.uploadInputStream(inputStream, targetPath, fileName);
    }
    
}
