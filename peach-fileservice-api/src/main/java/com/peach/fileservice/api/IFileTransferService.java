package com.peach.fileservice.api;

import com.peach.common.response.Response;
import com.peach.fileservice.ChunkUploadFile;
import com.peach.fileservice.entity.AttachDO;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 22:05
 */
public interface IFileTransferService {

    /**
     * 文件上传，支持切片上传，是否插入附件引用和附件信息
     * @param chunkUploadFile
     * @param attachDO
     * @param insertAttach
     * @param insertAttachRef
     * @return
     */
    Response upload(ChunkUploadFile chunkUploadFile, AttachDO attachDO, boolean insertAttach, boolean insertAttachRef);
}
