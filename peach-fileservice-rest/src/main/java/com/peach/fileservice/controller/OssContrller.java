package com.peach.fileservice.controller;


import com.peach.fileservice.impl.AbstractFileStorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/9 14:40
 */
@Slf4j
@RestController
@RequestMapping("/test/oss")
@Api(tags = "ossContrller",value = "存储测试")
public class OssContrller {

    @Resource
    AbstractFileStorageService fileStorageService;

    private static final String targetFilePath = "/costest/demo/file";

    private static final String localFilePath = System.getProperty("user.dir");


    @PostMapping()
    @ApiOperation(value = "cos文件上传")
    public Map upload(@RequestPart("file") MultipartFile file) throws IOException {

        String filePath = null;
        File targetFile =  convertMultipartFileToFile(file);
        try {
            filePath = fileStorageService.upload(new FileInputStream(targetFile), targetFilePath, file.getOriginalFilename());
        }catch (Exception ex){
            throw new RuntimeException("上传文件失败");
        }
        Map<String, String> props = new HashMap<>();
        props.put("msg","sucess");
        props.put("option","uploadFile");
        props.put("path",filePath);
        return props;
    }

    public File convertMultipartFileToFile(MultipartFile file) throws IOException {
        // 获取文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名为空！");
        }

        // 创建临时文件
        File convFile = File.createTempFile("temp-", originalFilename);

        // 将 MultipartFile 转换为 File
        file.transferTo(convFile);

        // 关闭 JVM 退出时自动删除（可选）
        convFile.deleteOnExit();

        return convFile;
    }


    @PostMapping("/downloadFile")
    @ApiOperation(value = "文件下载")
    public Map downloadFile(String key){
        boolean download = fileStorageService.download(key, localFilePath, "下载的文件.py");
        Map<String, Object> props = new HashMap<>();
        props.put("msg","sucess");
        props.put("download",download);
        return props;
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "文件删除")
    public Map delete(String key){

        boolean isDelete = fileStorageService.delete(key);
        Map<String, Object> props = new HashMap<>();
        props.clear();
        props.put("msg","sucess");
        props.put("option","delete");
        props.put("path",null);
        props.put("isDelete",isDelete);
        return props;
    }
}
