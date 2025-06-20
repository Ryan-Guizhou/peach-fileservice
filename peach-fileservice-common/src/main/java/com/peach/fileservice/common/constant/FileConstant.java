package com.peach.fileservice.common.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/20 16:17
 */
public interface FileConstant {

    String PATH_SEPARATOR = "/";

    String CONSTANT_PATH = "path";

    String CONSTANT_KEY = "key";

    String SEPARATOR_REG = "/{2,}";


    /**
     * 普通字符串 https:// 开头
     */
    String HTTPS_PREFIX = "https://";

    /**
     * 匹配 https:// 后的域名或 IP+端口
     */
    String HTTPS_DOMAIN_REGEX = "https://[^/]+";

    /**
     * 匹配 http:// 后的域名或 IP+端口
     */
    String HTTP_DOMAIN_REGEX = "http://[^/]+";

    /**
     * URL 参数起始符（正则）
     */
    String URL_PARAM_DELIMITER_REGEX = "\\?";

    /**
     * 分隔符统一正则（匹配多个连续 / 或 \）
     *
     */
    String PATH_SEPARATOR_REGEX = "[/\\\\]+";

    /**
     * 文件名称连接符
     */
    String FILE_NAME_SEPARATOR = "-";

}
