package com.peach.fileservice.common.util;

import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.common.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/20 17:16
 */
@Slf4j
public class StoreUtil {

    /**
     * 清洗目录路径，统一为 OSS Key 格式（仅用 `/` 连接的干净路径）
     * 示例输入: "/a\\b/c//d\\e?versionId=xxx"
     * 示例输出: "a/b/c/d/e"
     *
     * @param rawPath 原始路径（可能包含混合分隔符和URL参数）
     * @return 标准化 OSS Key 路径
     */
    public static String cleanFullKey(String rawPath) {
        if (StringUtils.isBlank(rawPath)) {
            return rawPath;
        }

        // 1. 移除 URL 参数部分（例如 ?version=123）
        String cleanPath = rawPath.split(FileConstant.URL_PARAM_DELIMITER_REGEX)[0];
        // 2. 用正则统一拆分所有 / 和 \，过滤空项后用 / 拼接
        return Arrays.stream(cleanPath.split(FileConstant.PATH_SEPARATOR_REGEX))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(FileConstant.PATH_SEPARATOR));
    }

    /**
     * 清洗路径，统一为 OSS Key 格式（仅用 `/` 连接的干净路径）
     * 示例输入: "/a\\b/c//d\\e?versionId=xxx"
     * 示例输出: "清洗路径，统一为 OSS Key 格式（仅用 `/` 连接的���净路径）"
     *
     * @param path
     * @return
     */
    public static String cleanPath(String path) {
       return  StringUtils.isBlank(path) ? path :
               (path.endsWith(FileConstant.PATH_SEPARATOR) ? path : path + FileConstant.PATH_SEPARATOR);
    }
    /**
     * 获取处理之后的fullKey
     * @param fileFullKey
     * @return
     */
    public static String getFileFullKey(String fileFullKey) {
        if (StringUtil.isBlank(fileFullKey)) {
            log.error("fileFullKey is blank");
            throw new FileUploadException("fileFullKey is blank");
        }
        return fileFullKey.replaceAll(FileConstant.SEPARATOR_REG, FileConstant.PATH_SEPARATOR);
    }

    /**
     * 获取处理之后的path
     * @param path 文件路径
     * @param fileName 文件名
     * @return
     */
    public static String buildFullFileKey(String path,String fileName) {
        return path + FileConstant.PATH_SEPARATOR + fileName;
    }

}
