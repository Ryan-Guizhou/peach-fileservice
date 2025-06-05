package com.peach.fileservice.impl;

import com.google.common.collect.Lists;
import com.peach.common.util.InputParamChecker;
import com.peach.common.util.PeachCollectionUtil;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.api.IAttachRefService;
import com.peach.fileservice.api.IAttachService;
import com.peach.fileservice.dao.AttachDao;
import com.peach.fileservice.entity.AttachDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 22:03
 */
@Slf4j
@Indexed
@Service
public class AttachServiceImpl implements IAttachService {

    @Resource
    private AttachDao attachDao;

    @Resource
    private IAttachRefService attachRefService;

    @Override
    public AttachDO getAttachDO(String attachId) {
        if (StringUtil.isBlank(attachId)) {
            log.error("attachId is null or empty");
            throw new IllegalArgumentException("attachId is null or empty");
        }
        return attachDao.selectById(attachId);
    }

    @Override
    public void insertAttachDO(AttachDO attachDO) {

    }

    @Override
    public List<AttachDO> selectByAttachIds(List<String> attachIdList) {
        Optional.ofNullable(attachIdList).orElseThrow(() -> new IllegalArgumentException("attachIdList is null"));
        List<AttachDO> attachDOList = attachDao.selectByIds(attachIdList);
        return PeachCollectionUtil.isEmpty(attachDOList) ? Lists.newArrayList() : attachDOList;
    }

    @Override
    public void modifyAttachDO(AttachDO attachDO) {
        try{
            InputParamChecker.of(attachDO).checkFields("id");
        }catch (Exception e){
            log.error("params error"+e.getMessage(), e);
            throw new RuntimeException(e);
        }
        attachDao.update(attachDO);
    }
}
