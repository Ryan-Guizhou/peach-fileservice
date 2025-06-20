package com.peach.fileservice.impl;

import com.amazonaws.services.s3.model.PartETag;
import com.peach.common.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.function.Function;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 文件上传抽象类，支持分片上传
 * @CreateTime 2025/6/20 14:03
 */
@Slf4j
public abstract class AbstractUploadService {

    /**
     * 完整文件上传
     * @param inputStream 文件输入流 / File Input Stream
     * @param targetFilePath 目标文件路径 / Target File Path
     * @param fileName 文件名 / File Name
     * @return
     */
    protected abstract boolean uploadFile(InputStream inputStream,String targetFilePath , String fileName);

    /**
     * 分片文件上传
     * @param uploadId 上传ID / Upload ID /example: 1234567890
     * @param chunk 分片号 / Chunk Number / example: 1
     * @param data 分片数据 / Chunk Data
     * @param partUploadPath 分片上传路径 / Part Upload Path /example: 1234567890/1
     * @param partUploadFileName 分片上传文件名 / Part Upload File Name / example: 1
     * @param fullFileKey 完整文件标识 / Full File Key / example: zip文件 /peach-common/yyyy-mm-dd/peach-common.zip
     * @return
     */
    protected abstract PartETag uploadFilePart(String uploadId, int chunk, byte[] data, String partUploadPath, String partUploadFileName, String fullFileKey);


    /**
     * 合并分片
     * @param uploadId 上传ID / Upload ID
     * @param chunkPath 分片路径 / Chunk Path
     * @param targetPath 目标路径 / Target Path
     * @param finalName 最终文件名 / Final File Name
     * @param dealwithChunkInfoFun 处理分片信息函数 / Deal with Chunk Info Function
     * @return
     */
    protected abstract boolean mergePart(String uploadId, String chunkPath, String targetPath, String finalName, Function<Response, Void> dealwithChunkInfoFun);



    /**
     * 获取分片数量
     * @param uploadId 上传ID / Upload ID
     * @param chunkPath 分片路径 / Chunk Path
     * @param fullFileKey 文件标识 / File Key / example: zip文件 /peach-common/yyyy-mm-dd/peach-common.zip
     * @return
     */
    protected abstract int getParCount(String uploadId,String chunkPath,String fullFileKey);


    /**
     * 检查分片是否存在
     * @param fullFileKey 完整文件标识 / Full File Key / example: zip文件 /peach-common/yyyy-mm-dd/peach-common.zip
     * @return
     */
    protected abstract boolean checkFileExist(String fullFileKey);


}
