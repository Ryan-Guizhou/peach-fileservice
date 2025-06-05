package com.peach.fileservice.qo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 22:13
 */
@Data
public class AttachRefQO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String attachId;

    private List<String> attachIdList;

    public AttachRefQO(String attachId) {
        this.attachId = attachId;
    }

    public AttachRefQO(List<String> attachIdList) {
        this.attachIdList = attachIdList;
    }
}
