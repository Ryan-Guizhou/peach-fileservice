package com.peach.fileservice.impl;

import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.common.util.IDGenerator;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.ChunkUploadFile;
import com.peach.fileservice.api.IAttachRefService;
import com.peach.fileservice.api.IAttachService;
import com.peach.fileservice.api.IFileTransferService;
import com.peach.fileservice.entity.AttachDO;
import com.peach.fileservice.entity.AttachRefDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/5 22:04
 */
@Slf4j
@Indexed
@Service
public class FileTransferServiceImpl implements IFileTransferService {

    @Resource
    private IAttachService attachService;

    @Resource
    private IAttachRefService attachRefService;

    @Resource
    private AbstractFileStorageService fileStorageService;


    @Override
    public Response upload(ChunkUploadFile chunkUploadFile, AttachDO attachDO, boolean insertAttach, boolean insertAttachRef) {
        Integer chunks = chunkUploadFile.getChunks();

        String fileUrl = StringUtil.EMPTY;
        if (chunks == null) {
            fileUrl = unChunck(chunkUploadFile);
        }else {
            fileUrl = chunck(chunkUploadFile);
        }

        // 如果需要插入附件信息
        if (insertAttach) {
            AttachDO existAttchDO = attachService.getAttachDO(attachDO.getId());
            if (existAttchDO == null) {
                // 插入
                existAttchDO = new AttachDO();
                BeanUtils.copyProperties(attachDO, existAttchDO);
                existAttchDO.setId(IDGenerator.UUID());
                attachService.insertAttachDO(existAttchDO);
            }else {
                // 修改
                existAttchDO.setFilePath(fileUrl);
                attachService.modifyAttachDO(existAttchDO);
            }
        }
        // 如果需要插入附件引用信息
        if (insertAttachRef) {
            AttachRefDO attachRefDO = new AttachRefDO();
            attachRefDO.setId(IDGenerator.UUID());
//            attachRefDO.setBusinessId();
//            attachRefDO.setBusinessCode();
//            attachRefDO.setBusinessName();
            attachRefDO.setAttachId(attachDO.getId());
            attachRefDO.setIsDeleted(PubCommonConst.LOGIC_FLASE);

        }

        return Response.success().setData(fileUrl);
    }

    /**
     * 不分片上传
     * @param chunkUploadFile 分片上传参数
     * @return 上传之后带签名的url
     */
    private String unChunck(ChunkUploadFile chunkUploadFile){

        return null;
    }

    /**
     * 分片上传
     * @param chunkUploadFile 分片上传参数
     * @return 上传之后的带签名的url
     */
    private String chunck(ChunkUploadFile chunkUploadFile){
        return null;
    }
}
