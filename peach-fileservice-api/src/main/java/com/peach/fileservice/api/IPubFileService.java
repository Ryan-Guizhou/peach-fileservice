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
public interface IPubFileService {

    /**
     * 文件上传
     * @param chunkUploadFile 分片上传参数
     * @param attachDO 附件信息
     * @param insertAttach 是否插入附件信息
     * @param insertAttachRef 是否插入附件引用信息
     * @return
     */
    Response upload(ChunkUploadFile chunkUploadFile, AttachDO attachDO, boolean insertAttach, boolean insertAttachRef);
}
