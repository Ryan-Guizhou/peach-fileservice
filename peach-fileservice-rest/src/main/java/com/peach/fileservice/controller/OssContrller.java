package com.peach.fileservice.controller;


import com.peach.fileservice.AbstractFileStorageService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
@Api(tags = "文件上传", value = "文件上传")
public class OssContrller {

    @Autowired
    AbstractFileStorageService fileStorageService;

    @PostMapping()
    @ApiOperation(value = "文件上传")
    public Map upload(@RequestPart("file") MultipartFile file) throws IOException {

        String filePath = null;
        File targetFile =  convertMultipartFileToFile(file);
        try {
            filePath = fileStorageService.upload(new FileInputStream(targetFile), "/data/file/test/213213", file.getOriginalFilename());
        }catch (Exception ex){
            throw new RuntimeException("上传文件失败");
        }
          Map<String, String> props = new HashMap<>();
        props.clear();
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


    @GetMapping("/downloadFile")
    @ApiOperation(value = "文件下载")
    public Map upload(){


        Map<String, String> props = new HashMap<>();
//        if (flag){
//            log.error("下载成功");
//            props.put("path","下载成功");
//        }
        props.clear();
        props.put("msg","sucess");
        props.put("option","uploadFile");
        props.put("path","filePath");
        return props;
    }

    @DeleteMapping("/delete/{source}")
    @ApiOperation(value = "文件下载")
    public Map upload(@PathVariable("source") String source){



        fileStorageService.delete(source);

        Map<String, String> props = new HashMap<>();
        props.clear();
        props.put("msg","sucess");
        props.put("option","delete");
        props.put("path",null);
        return props;
    }
}
