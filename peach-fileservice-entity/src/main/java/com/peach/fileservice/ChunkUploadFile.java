package com.peach.fileservice;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 22:36
 */
@Data
public class ChunkUploadFile implements Serializable {

    private static final long serialVersionUID = 1L;


    private Integer chunks;

    private Integer chunkIndex;

    private Integer chunkSize;

    private PartFile partFile;
}
