package com.peach.fileservice.impl;

import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Lists;
import com.peach.common.util.PeachCollectionUtil;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.api.IAttachRefService;
import com.peach.fileservice.dao.AttachRefDao;
import com.peach.fileservice.entity.AttachRefDO;
import com.peach.fileservice.qo.AttachRefQO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 22:03
 */
@Slf4j
@Indexed
@Service
public class AttachRefServiceImpl implements IAttachRefService {

    @Resource
    private AttachRefDao attachRefDao;

    @Override
    public List<AttachRefDO> getAttachRefDO(String attachId) {
        if(StringUtil.isBlank(attachId)) {
            log.error("attachId is null or empty");
            return Lists.newArrayList();
        }
        AttachRefQO attachRefQO = new AttachRefQO(attachId);
        return selectByQO(attachRefQO);
    }

    @Override
    public void insertAttachRefDO(AttachRefDO attachRefDO) {
        if(ObjectUtil.isNull(attachRefDO)) {
            log.error("attachRefDO is null");
            return;
        }
        attachRefDao.insert(attachRefDO);
    }

    @Override
    public List<AttachRefDO> selectByAttachIds(List<String> attachIdList) {
        if(PeachCollectionUtil.isEmpty(attachIdList)) {
            log.error("attachIdList is null or empty");
            return Lists.newArrayList();
        }
        AttachRefQO attachRefQO = new AttachRefQO(attachIdList);
        return selectByQO(attachRefQO);
    }

    private List<AttachRefDO> selectByQO(AttachRefQO attachRefQO) {
        if(ObjectUtil.isNull(attachRefQO)) {
            log.error("attachRefQO is null");
            return Lists.newArrayList();
        }
        List<AttachRefDO> attachRefDOList = attachRefDao.selectByQO(attachRefQO);
        return PeachCollectionUtil.isEmpty(attachRefDOList) ? Lists.newArrayList() : attachRefDOList;
    }
}
