package com.peach.fileservice.common.enums;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/9 15:30
 */
public enum StroageEnum {
    ALI_OSS("aliOss", "阿里云oss"),
    QINIU_OSS("qiniuOss", "七牛云oss"),
    TENCENT_OSS("tencentOss", "腾讯云oss"),
    MINIO_OSS("minioOss", "minioOss"),
    S3("s3", "只要是满足s3协议的存储均可以使用这种方式实例化"),;

    private String code;
    private String desc;

    StroageEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
