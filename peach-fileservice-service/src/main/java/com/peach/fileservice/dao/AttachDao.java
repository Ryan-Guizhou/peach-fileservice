package com.peach.fileservice.dao;

import com.peach.common.BaseDao;
import com.peach.common.anno.MyBatisDao;
import com.peach.fileservice.AttachDO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/16 11:28
 */
@Indexed
@MyBatisDao
public interface AttachDao extends BaseDao<AttachDO> {


}
