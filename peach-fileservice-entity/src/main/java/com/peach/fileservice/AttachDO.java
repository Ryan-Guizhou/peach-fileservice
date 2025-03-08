package com.peach.fileservice;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/9 18:26
 */
@Data
@Table(name = "PEACH_ATTACH")
@ApiModel(description = "附件对象")
public class AttachDO implements Serializable {

    private static final long serialVersionUID = -3918404837627972195L;

    @Id
    @Column(name = "ID")
    @ApiModelProperty(value = "用户ID")
    private String id;

    @Column(name = "FILE_NAME")
    @ApiModelProperty(value = "文件名称")
    private String fileName;

    @Column(name = "FILE_PATH")
    @ApiModelProperty(value = "文件存储路径")
    private String filePath;

    @Column(name = "FILE_SIZE")
    @ApiModelProperty(value = "文件大小")
    private double fileSize;

    @Column(name = "ORIGINAL_FILE_NAME")
    @ApiModelProperty(value = "完整的文件名称")
    private String originalFileName;

    @Column(name = "CREATED_TIME")
    @ApiModelProperty(value = "创建时间")
    private String createdTime;

    @Column(name = "MODIFIED_TIME")
    @ApiModelProperty(value = "更新时间")
    private String modifiedTime;

    @Column(name = "CREATOR")
    @ApiModelProperty(value = "创建人")
    private String creator;

    @Column(name = "CREATOR_NAME")
    @ApiModelProperty(value = "创建人名称")
    private String CREATOR_NAME;

    @Column(name = "MODIFIER")
    @ApiModelProperty(value = "修改人")
    private String modifier;


    @Column(name = "MODIFIER_NAME")
    @ApiModelProperty(value = "修改人名称")
    private String modifierName;

    @Column(name = "FILE_TYPE")
    @ApiModelProperty(value = "文件类型")
    private String fileType;

    @Column(name = "IS_DELETED")
    @ApiModelProperty(value = "是否删除")
    private boolean isDeleted;

//    public static void main(String[] args) {
//        System.out.print(System.getProperty("user.dir"));
////        System.out.print(MapperGenerator.genMapper(AttachDO.class));
//        // 使用hutool工具类 获取当前类的路径
//        String prefixPath = "使用绝对路径";
//        MapperGenerator.genMapperToFile(AttachDO.class, "AttchDao",prefixPath);
//    }

}
