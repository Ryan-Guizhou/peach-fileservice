package com.peach.fileservice.impl;

import cn.hutool.core.io.FileUtil;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.DiskDO;
import com.peach.fileservice.common.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/9 14:43
 */
@Slf4j
public abstract class AbstractFileStorageService{

    protected static final String PATH_SEPARATOR = "/";

    protected static final long EXPIRATION = 3600L * 1000 * 24 * 365 * 2;

    // 创建重试器
    protected static Retryer<File> retryer = RetryerBuilder.<File>newBuilder()
            .retryIfResult(result -> result == null || !FileUtil.exist(result))  // 失败条件：返回 false 时重试
            .retryIfException() // 任何异常都重试
            .withStopStrategy(StopStrategies.stopAfterAttempt(3)) // 最多重试 3 次
            .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS)) // 每次重试间隔 2 秒
            .build();

    /**
     * 替换url 中的签名信息
     * before replace /dataset/128/151/compressed/5/sjt-sub-14-6.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20240821T030150Z&X-Amz-SignedHeaders=host&X-Amz-Expires=2592000&X-Amz-Credential=F8AOFS0MTN0HXBYV35RD%2F20240821%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Signature=41bb33e53aa055e1b82f5208f6f94f153399c3ef9bdb163571715ba53b9c9080
     * after  replace /dataset/128/151/compressed/5/sjt-sub-14-6.png
     */
    protected static final int URL_TAKE_SIGN_NO = 0;
    protected static final int URL_TAKE_SIGN_YES = 1;

    /**
     * 复制文件夹以及文件夹下面所有内容
     *
     * @param sourceDir 源文件夹
     * @param targetDir 目标目录
     * @return boolean
     */
    public abstract boolean copyDir(String sourceDir, String targetDir);

    /**
     * 下载某个文件夹下所有的文件
     *
     * @param sourceDir 资源文件夹
     * @param localDir  本地文件夹
     * @return boolean
     */
    public abstract boolean downDir(String sourceDir, String localDir);

    /**
     * 文件上传接口
     *
     * @param inputStream 文件流
     * @param targetPath  目标上传路径
     * @param fileName    文件名称 带后缀
     * @return: java.lang.String 带签名存储路径
     * @author: pc
     */
    public abstract String upload(InputStream inputStream, String targetPath, String fileName);

    /**
     * 文件上传接口
     *
     * @param content    文件内容
     * @param targetPath 目标上传路径
     * @param fileName   文件名称 带后缀
     * @return: java.lang.String  带签名存储路径
     * @author: pc
     */
    public abstract String upload(String content, String targetPath, String fileName);

    /**
     * 文件上传接口
     *
     * @param file       文件，一个或者多个
     * @param targetPath 目标上传路径
     * @return: java.util.List  带签名存储路径
     * @author: pc
     */
    public abstract List<String> upload(File[] file, String targetPath);

    /**
     * 文件上传接口
     *
     * @param file       文件
     * @param targetPath 目标上传路径
     * @param fileName   文件名称 带后缀
     * @return: java.lang.String  带签名存储路径
     */
    public abstract String upload(File file, String targetPath, String fileName);


    /**
     * 从 targetPath 下载文件到本地指定目录
     *
     * @param targetPath 要下载的目标资源路径,/data/file/test/6.jpg?Expires=1803807824&OSSAccessKeyId=LTAI5tQiXiDypb3HDKq2uGKi&Signature=qDHJTu3G441I6WvId6RKHOdhvEs%3D
     * @param localPath 本地的存储路径
     * @param fileName 下载之后的文件名
     * @return: void
     * @author: pc
     */
    public abstract boolean download(String targetPath, String localPath, String fileName);


    /**
     * 通过文件路径获取 文件流
     *
     * @param targetPath （样例：/data/nfsdata/ 或者 /data/nfsdata）
     * @param fileName   (样例：test.yml)
     * @return: java.io.InputStream
     * @author: pc
     */
    public abstract InputStream getInputStream(String targetPath, String fileName);


    /**
     * 通过文件路径获取 文件流(文件全路径)
     *
     * @param key (样例：/nacos/config/test.yml)
     * @return
     */
    public abstract InputStream getInputStreamByKey(String key);

    /**
     * 删除文件或者文件夹
     *
     * @param key 目标地址 (样例：/nacos/config/test.yml 或者 /nacos/config/ )
     * @return
     */
    public abstract boolean delete(String key);


    /**
     * 复制文件 （暂未实现）
     *
     * @param currentPath
     * @param targetPath
     * @return
     */
    public abstract boolean copyFile(String currentPath, String targetPath);


    /**
     * 通过文件路径+文件名 获取url（key样例：/data/nfsdata/15374511/abc/大图.zip）
     *
     * @param key
     * @return
     */
    public abstract String getUrlByKey(String key);

    /**
     * 通过文件路径+文件名 获取带签名路径（key样例：/data/nfsdata/15374511/abc/大图.zip）
     *
     * @param key
     * @return
     */
    public abstract String getPathByKey(String key);

    /**
     * 为文件设置公共读 针对oss、ceph 等分布式存储本
     *
     * @param path
     * @return
     */
    public abstract void setPublicReadAcl(String path);

    /**
     * 获取当前服务器磁盘空间 使用情况
     *
     * @param path 目标路径
     * @return DiskEntity 实体
     */
    public DiskDO getCurrentDiskSpace(String path) {
        try {
            if (StringUtils.isBlank(path)) {
                path = File.separator;
            }
            FileStore fileStore = Files.getFileStore(Paths.get(path));
            long totalSpace = fileStore.getTotalSpace();
            long usableSpace = fileStore.getUsableSpace();
            long unallocatedSpace = fileStore.getUnallocatedSpace();
            String type = fileStore.type();
            double total = bytesToGB(totalSpace);
            double usable = bytesToGB(usableSpace);
            double unallocated = bytesToGB(unallocatedSpace);
            return DiskDO.builder().path(path).type(type).usableSpace(usable).unallocatedSpace(unallocated).totalSpace(total).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * bytes to GB
     *
     * @param bytes 输入字节
     * @return 转换后数据 GB
     */
    private double bytesToGB(long bytes) {
        BigDecimal size = new BigDecimal(bytes);
        BigDecimal gb = size.divide(BigDecimal.valueOf(1024 * 1024 * 1024), 2, RoundingMode.HALF_UP);
        return gb.doubleValue();
    }

    /**
     * 获取oss存储路径 将格式转换为 a/b/c/
     * @param dirName
     * @return
     */
    protected String getOssKey(String dirName) {
        if (StringUtils.isBlank(dirName)) {
            return dirName;
        }

        dirName = decodeKey(dirName);

        // 去除查询参数 ?xxx=yyy
        String rawPath = dirName.split("\\?")[0];

        // 统一路径分隔符，将 \ 替换为 /
        String normalizedPath = normalizePath(rawPath);

        // 使用 Stream 去除空段并重新拼接
        String cleaned = Arrays.stream(normalizedPath.split(FileConstant.PATH_SEPARATOR))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(FileConstant.PATH_SEPARATOR));
        log.debug("[getOssKey] original: {}, cleaned: {}", dirName, cleaned);
        return cleaned;
    }

    /**
     * 解码key
     * @param key
     * @return
     * @throws UnsupportedEncodingException
     */
    protected String decodeKey(String key) {
        if (StringUtils.isBlank(key)) {
            return key;
        }
        try {
            return URLDecoder.decode(key, PubCommonConst.UTF_8);
        }catch (Exception e){
            log.error("[decodeKey] decode error for key: {}", key, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 替换所有的//为/
     * @param key
     * @return
     */
    private String normalizePath(String key){
        if (StringUtils.isBlank(key)) {
            return key;
        }
        String result = key.trim().replace("\\", "/")        // 将所有反斜杠替换为正斜杠
                .replaceAll("/\\./", "/")
                .replaceAll("/{2,}", "/");          // 多个 / 替换为一个

        log.debug("[normalizePath] input: [{}], normalized: [{}]", key, result);
        return result;
    }


    /**
     * 检查文件路径是否包含非法字符 防止删除根目录、父目录或非法路径
     * @param key
     * @return
     */
    protected boolean isHasIllegalChar(String key) {
        try {
            if (StringUtil.isBlank(key)) {
                throw new IllegalArgumentException("Path cannot be blank");
            }

            // 清理路径前后的空格，并统一为正斜杠
            key = normalizePath(key);

            // 不允许删除根目录或上级目录
            if (key.equals("/") || key.equals("..") || key.equals(".") || key.equals("")) {
                throw new IllegalArgumentException("Path cannot be /, .., or .");
            }

            // 不允许路径中包含 .. 或 ./ 等不规范路径
            if (key.contains("/../") || key.contains("/./") || key.contains("//")) {
                throw new IllegalArgumentException("Path contains invalid sequences like ../ or //");
            }

            // 可选：不允许路径结尾为 .. 或 .
            if (key.endsWith("/..") || key.endsWith("/.")) {
                throw new IllegalArgumentException("Path should not end with .. or .");
            }
        }catch (Exception e){
            log.error("check file path is illegal,path:{}",key,e);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * 移除url中的host,支持http、https
     * @param url 附件的完整路径
     * @return
     */
    protected String removeUrlHost(String url) {
        if (StringUtils.isBlank(url)){
            log.error("this url is blank,can't be remove");
            return url;
        }
        if (url.contains(FileConstant.HTTPS_PREFIX)) {
            return url.replaceAll(FileConstant.HTTPS_DOMAIN_REGEX, StringUtil.EMPTY);
        }
        if (url.contains(FileConstant.HTTP_PREFIX)) {
            return url.replaceAll(FileConstant.HTTP_DOMAIN_REGEX, StringUtil.EMPTY);
        }
        return url;
    }


    /**
     * 用于删除时 清洗文件或文件夹的路径
     * @param key 文件或文件夹的路径
     * @return
     */
    protected String handlerKeyPrefix(String key) {
        if (StringUtil.isBlank(key)) {
            return key;
        }
        key = normalizePath(key);
        key = key.replaceFirst("^/", "");
        key = decodeKey(key);

        String prefix = key.contains("?") ? key.split("\\?")[0] : key;
        if (!prefix.contains(".") && !prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix;
    }

    /**
     * 获取处理之后的path
     * @param path 文件路径
     * @param fileName 文件名
     * @return
     */
    protected String buildKey(String path,String fileName) {
        path = normalizePath(path);
        if (!path.endsWith(FileConstant.PATH_SEPARATOR)) {
            path += FileConstant.PATH_SEPARATOR;
        }
        String fullKey = path + fileName;
        log.debug("[buildKey] path: [{}], fileName: [{}], fullKey: [{}]", path, fileName, fullKey);
        return fullKey;
    }

    /**
     * 标准化目录路径
     * @param dir
     * @return
     */
    protected String normalizeDirectory(String dir) {
        if (dir == null || dir.trim().isEmpty()) {
            return "";
        }

        dir = dir.trim();

        // 去除开头的 "/"
        if (dir.startsWith(FileConstant.PATH_SEPARATOR)) {
            dir = dir.substring(1);
        }

        // 添加结尾的 "/"
        if (!dir.endsWith(FileConstant.PATH_SEPARATOR)) {
            dir += FileConstant.PATH_SEPARATOR;
        }

        return normalizePath(dir);
    }


    /**
     * 定义本地上传的逻辑，其他存储方式需要重写此方法
     * @param file
     * @param targetPath
     * @param fileName
     * @return
     */
    protected String uploadFile(File file, String targetPath, String fileName) {
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
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName) {
        // 1、上传文件
        String standardPath = normalizeDirectory(targetPath) ;
        String key = buildKey(standardPath, fileName);

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
            return standardPath.replace(File.separator, PATH_SEPARATOR) + URLEncoder.encode(fileName, PubCommonConst.UTF_8)
                    .replaceAll("\\+", "%20") + "?timestamp=" + System.currentTimeMillis();
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException"+e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过键获取原始URL或路径, 默认返回本地的路径，其他的实现需要重写该方法
     * @param key 文件键 / File key
     * @param isUrl 是否返回URL或者路径
     * @return URL 或者路径 / URL or Path
     */
    protected String getOrgUrlByKey(String key,boolean isUrl) {
       return StringUtil.EMPTY;
    }
}
