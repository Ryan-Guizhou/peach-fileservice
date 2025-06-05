package com.peach.fileservice.enums;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 21:44
 */
public interface AttachEnum {

    /**
     * 附件存储枚举
     */
    enum AttachStorageEnum implements AttachEnum {

        OSS("OSS","阿里云存储"),
        COS("COS","腾讯云存储"),
        OBS("OBS","华为云存储"),
        MONGO("MONGO","MONGO存储"),
        S3("S3","亚马逊存储"),
        LOCAL("LOCAL","本地存储"),
        MINIO("MINIO","MINIO存储");

        private final String code;

        private final String value;

        private static final Map<String, AttachStorageEnum> map = new HashMap<String, AttachStorageEnum>();

        static {
            Arrays.stream(AttachStorageEnum.values()).forEach(e -> map.put(e.code, e));
        }

        AttachStorageEnum(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 附件上传类型枚举
     */
    enum AttachUploadTypeEnum implements AttachEnum {

        LOCALHOST("","本地上传"),
        FTP("FTP","FTP上传"),
        INDEX("INDEX","索引上传");

        private final String code;

        private final String value;

        private static final Map<String, AttachUploadTypeEnum> map = new HashMap<String, AttachUploadTypeEnum>();

        static {
            Arrays.stream(AttachUploadTypeEnum.values())
                    .forEach(e -> map.put(e.code, e));
        }

        AttachUploadTypeEnum(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }
}
