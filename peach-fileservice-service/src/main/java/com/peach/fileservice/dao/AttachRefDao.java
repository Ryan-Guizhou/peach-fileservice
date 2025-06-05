package com.peach.fileservice.dao;

import com.peach.common.BaseDao;
import com.peach.common.anno.MyBatisDao;
import com.peach.fileservice.entity.AttachRefDO;
import com.peach.fileservice.qo.AttachRefQO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 21:39
 */
@Indexed
@MyBatisDao
public interface AttachRefDao extends BaseDao<AttachRefDO> {

    List<AttachRefDO> selectByQO(AttachRefQO qo);

}
