package com.peach.fileservice.api.upload;

import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;

import java.io.InputStream;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/20 14:47
 */
public interface S3Storage {

    /**
     * 上传分片 / Upload part
     * @param uploadId 文件上传标识 / File upload identifier
     * @param chunk 分片编号 / Chunk number
     * @param data 分片数据 / Chunk data
     * @param fileFullKey 完整的文件路径 / Complete file path / example: /test/123/123.jpg
     * @return
     */
    PartETag uploadPart(String uploadId, int chunk, byte[] data, String fileFullKey);

    /**
     * 上传文件
     * @param inputStream 文件流 / File stream
     * @param targetPath 文件路径 / File path
     * @param fileName 文件名 / File name
     * @return
     */
    String uploadFile(InputStream inputStream, String targetPath, String fileName);

    /**
     * 合并分片文件 / Merge part files
     * @param uploadId 文件上传标识 / File upload identifier
     * @param partETagList 分片列表 / Part list
     * @param fileFullKey 完整的文件路径 / Complete file path / example: /test/123/123.jpg
     * @return
     */
    boolean mergePart(String uploadId, List<PartETag> partETagList, String fileFullKey);

    /**
     * 检测文件是否存在
     * @param fileFullKey
     * @return
     */
    boolean checkFileExist(String fileFullKey);

    /**
     * 检测分片是否存在
     * @param chunk 分片编号 / Chunk number
     * @param uploadId 文件上传标识 / File upload identifier
     * @param fileFullKey 完整的文件路径 / Complete file path / example: /test/123/123.jpg
     * @return
     */ 
    boolean checkPartExist(int chunk, String uploadId, String fileFullKey);

    /**
     * 获取分片列表
     * @param uploadId 文件上传标识 / File upload identifier
     * @param fileFullKey 完整的文件路径 / Complete file path / example: /test/123/123.jpg
     * @return
     */
    List<PartSummary> partSummaryList(String uploadId, String fileFullKey);
}
