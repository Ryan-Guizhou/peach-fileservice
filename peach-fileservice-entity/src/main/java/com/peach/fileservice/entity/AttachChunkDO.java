package com.peach.fileservice.entity;

import com.peach.common.generator.MapperGenerator;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 分片上传实体表，对应每一个文件上传请求的每一个分片
 * @CreateTime 2025/6/5 21:26
 */
@Data
@Table(name = "PEACH_ATTACH_CHUNK")
public class AttachChunkDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @ApiModelProperty(value = "主键")
    private String id;

    @Column(name = "UPLOAD_ID")
    @ApiModelProperty(value = "上传会话ID（每次上传唯一）")
    private String uploadId;

    @Column(name = "FILE_MD5")
    @ApiModelProperty(value = "文件MD5（秒传标识）")
    private String fileMd5;

    @Column(name = "CHUNK_INDEX")
    @ApiModelProperty(value = "当前分片序号，从0开始")
    private Integer chunkIndex;

    @Column(name = "CHUNK_SIZE")
    @ApiModelProperty(value = "当前分片大小")
    private Long chunkSize;

    @Column(name = "TOTAL_CHUNKS")
    @ApiModelProperty(value = "总分片数")
    private Integer totalChunks;

    @Column(name = "FILE_NAME")
    @ApiModelProperty(value = "原始文件名")
    private String fileName;

    @Column(name = "FILE_SIZE")
    @ApiModelProperty(value = "原始文件大小")
    private Long fileSize;

    @Column(name = "STORAGE_PATH")
    @ApiModelProperty(value = "临时分片存储路径")
    private String storagePath;

    @Column(name = "IS_MERGED")
    @ApiModelProperty(value = "是否合并完成")
    private Integer isMerged;


    public static void main(String[] args) {
        System.out.println(MapperGenerator.genMapper(AttachChunkDO.class));
    }
}
