package com.peach.fileservice.api;

import com.peach.fileservice.entity.AttachDO;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 21:42
 */
public interface IAttachService {

    /**
     * 根据附件id查找附件
     * @param attachId
     * @return
     */
    AttachDO getAttachDO(String attachId);


    /**
     * 保存附件信息
     * @param attachDO
     */
    void insertAttachDO(AttachDO attachDO);


    /**
     * 根据附件ID集合查找附件
     * @param attachIdList
     * @return
     */
    List<AttachDO> selectByAttachIds(List<String> attachIdList);


    /**
     * 修改附件信息
     * @param attachDO
     */
    void modifyAttachDO(AttachDO attachDO);

}
