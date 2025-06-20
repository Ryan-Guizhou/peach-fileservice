package com.peach.fileservice.impl.factory;

import com.peach.fileservice.impl.AbstractUploadService;
import com.peach.fileservice.impl.upload.UploadCloudStore;
import com.peach.fileservice.impl.upload.UploadLocalStore;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 静态工厂方法，生产不同的上传服务
 * @CreateTime 2025/6/20 14:04
 */
@Slf4j
public class UploadServiceFactory {

    /**
     * 获取云存储服务
     * @return
     */
    public static AbstractUploadService getUploadColudService() {
        return new UploadCloudStore();
    }

    /**
     * 获取云存储服务
     * @return
     */
    public static AbstractUploadService getUploadLocalStore() {
        return new UploadLocalStore();
    }
}
