package com.peach.fileservice.api;

import com.peach.fileservice.entity.AttachRefDO;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 21:42
 */
public interface IAttachRefService {

    /**
     * 根据附件id查找附件
     * @param attachId
     * @return
     */
    List<AttachRefDO> getAttachRefDO(String attachId);


    /**
     * 保存附件信息
     * @param attachRefDO
     */
    void insertAttachRefDO(AttachRefDO attachRefDO);


    /**
     * 根据附件ID集合查找附件
     * @param attachIdList
     * @return
     */
    List<AttachRefDO> selectByAttachIds(List<String> attachIdList);
}
