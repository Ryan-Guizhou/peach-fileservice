package com.peach.fileservice.common.util;

import com.peach.common.constant.PubCommonConst;
import com.peach.fileservice.common.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/15 14:19
 */
@Slf4j
public class FileUtils {


    public static File convertMultipartFileToFile(MultipartFile file) throws IOException {
        // 获取文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名为空！");
        }

        // 创建临时文件
        File convFile = File.createTempFile("temp-", originalFilename);

        // 将 MultipartFile 转换为 File
        file.transferTo(convFile);

        // 关闭 JVM 退出时自动删除（可选）
        convFile.deleteOnExit();

        return convFile;
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
        boolean isAbsolutePath = cn.hutool.core.io.FileUtil.isAbsolutePath(key);
        if (!isAbsolutePath) {
            key = FileConstant.PATH_SEPARATOR + key;
        }
        log.info("absolutePath key:" + key);
        return key;
    }
}
