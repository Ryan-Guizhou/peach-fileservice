package com.peach.fileservice.dao;

import com.peach.common.BaseDao;
import com.peach.common.anno.MyBatisDao;
import com.peach.fileservice.entity.AttachChunkDO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 21:38
 */
@Indexed
@MyBatisDao
public interface AttachChunkDao extends BaseDao<AttachChunkDO> {

}
