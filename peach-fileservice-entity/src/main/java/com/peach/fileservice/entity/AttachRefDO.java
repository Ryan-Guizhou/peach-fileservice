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
 * @Description 附件引用表 为了实现秒传设计 如果一个文件存在多个引用只删除对应的引用关系，不删除原始物理存储关系
 * @CreateTime 2024/10/9 18:26
 */
@Data
@Table(name = "PEACH_ATTACH_REF")
public class AttachRefDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @ApiModelProperty("主键")
    private String id;

    @Column(name = "BUSINESS_ID")
    @ApiModelProperty(value = "业务主键")
    private String businessId;

    @Column(name = "BUSINESS_CODE")
    @ApiModelProperty(value = "业务模块编码")
    private String businessCode;

    @Column(name = "BUSINESS_NAME")
    @ApiModelProperty(value = "业务模块名称")
    private String businessName;

    @Column(name = "ATTACH_ID")
    @ApiModelProperty(value = "附件ID")
    private String attachId;

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
    private String creatorName;

    @Column(name = "MODIFIER")
    @ApiModelProperty(value = "修改人")
    private String modifier;

    @Column(name = "MODIFIER_NAME")
    @ApiModelProperty(value = "修改人名称")
    private String modifierName;

    @Column(name = "IS_DELETED")
    @ApiModelProperty(value = "是否删除")
    private Integer isDeleted;

    public static void main(String[] args) {
        System.out.println(MapperGenerator.genMapper(AttachRefDO.class));
    }
}
