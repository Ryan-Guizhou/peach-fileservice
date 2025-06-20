package com.peach.fileservice.impl.upload;

import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.peach.common.response.Response;
import com.peach.common.util.PeachCollectionUtil;
import com.peach.fileservice.api.upload.S3Storage;
import com.peach.fileservice.common.util.StoreUtil;
import com.peach.fileservice.impl.AbstractUploadService;
import com.peach.fileservice.impl.Invocation.S3InvocationHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 基于s3协议的分片存储实现
 * @CreateTime 2025/6/20 14:07
 */
@Slf4j
public class UploadCloudStore extends AbstractUploadService {

    private static volatile S3Storage INSTANCE;

    /**
     * 单例模式获取实例 / Singleton pattern to get instance
     * @return
     */
    private static S3Storage getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (UploadCloudStore.class) {
            if (INSTANCE == null) {
                INSTANCE = (S3Storage) Proxy.newProxyInstance(
                        getClassLoader(S3Storage.class)
                        , new Class<?>[]{S3Storage.class},
                        new S3InvocationHandler<>(new S3StoreServiceImpl())
                );
            }
            return INSTANCE;
        }
    }

    /**
     * 获取类加载器 / Get class loader
     * @param type 类型 / Type
     * @return
     */
    private static ClassLoader getClassLoader(Class<?> type) {
        ClassLoader loader = type.getClassLoader();
        if (loader != null) {
            return loader;
        }
        loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            return loader;
        }
        return ClassLoader.getSystemClassLoader();
    }

    @Override
    protected boolean uploadFile(InputStream inputStream, String targetFilePath, String fileName) {
        String url = getInstance().uploadFile(inputStream, targetFilePath, fileName);
        return StringUtils.isNotBlank(url) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected PartETag uploadFilePart(String uploadId, int chunk, byte[] data, String partUploadPath, String partUploadFileName, String fullFileKey) {
        return getInstance().uploadPart(uploadId, chunk, data, fullFileKey);
    }

    @Override
    protected boolean mergePart(String uploadId, String chunkPath, String targetPath, String finalName, Function<Response, Void> dealwithChunkInfoFun) {
        List<PartETag> eTags = new ArrayList<>();
        String fullKey = StoreUtil.buildFullFileKey(targetPath,finalName);
        List<PartSummary> partSummaryList = getInstance().partSummaryList(uploadId, fullKey);
        for (PartSummary partSummary : partSummaryList) {
            PartETag partETag = new PartETag(partSummary.getPartNumber(), partSummary.getETag());
            eTags.add(partETag);
        }
        return getInstance().mergePart(uploadId, eTags, fullKey);
    }

    @Override
    protected int getParCount(String uploadId, String chunkPath, String fullFileKey) {
        List<PartSummary> partSummaryList = getInstance().partSummaryList(uploadId, fullFileKey);
        return PeachCollectionUtil.isEmpty(partSummaryList) ? 0 : partSummaryList.size();
    }

    @Override
    protected boolean checkFileExist(String fullFileKey) {
        return getInstance().checkFileExist(fullFileKey);
    }

}
