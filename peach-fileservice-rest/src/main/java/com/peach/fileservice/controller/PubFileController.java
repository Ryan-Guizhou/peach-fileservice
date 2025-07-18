package com.peach.fileservice.controller;

import cn.hutool.core.io.FileUtil;
import com.jcraft.jsch.ChannelSftp;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.common.util.FileUtils;
import com.peach.fileservice.common.util.NasUtil;
import com.peach.fileservice.common.util.shh.NasSftpService;
import com.peach.fileservice.common.util.shh.bean.SshChannel;
import com.peach.fileservice.config.FileProperties;
import com.peach.fileservice.impl.AbstractFileStorageService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 通用的文件上传、删除
 * @CreateTime 2025/6/15 14:16
 */
@Slf4j
@RestController
@RequestMapping("/pub")
public class PubFileController {

    @Resource
    private AbstractFileStorageService fileStorageService;

    @Resource
    private FileProperties fileProperties;



    @PostMapping("/upload")
    @ApiOperation("上传文件，返回文件地址")
    public Response upload(@RequestPart("file") MultipartFile file) {
        String filePath = StringUtil.EMPTY;
        String targetFilePath = fileProperties.getPubDirPrefix() + FileConstant.PATH_SEPARATOR  + System.currentTimeMillis();
        try {
            File targetFile = FileUtils.convertMultipartFileToFile(file);
            filePath = fileStorageService.upload(new FileInputStream(targetFile), targetFilePath, file.getOriginalFilename());
            filePath = fileStorageService.getUrlByKey(filePath);
        }catch (Exception ex){
            throw new RuntimeException("上传文件失败");
        }
        return Response.success().setData(filePath).setMsg("文件上传成功");
    }


    @DeleteMapping("/delete")
    @ApiOperation("根据key删除文件")
    public Response upload(String key) {
        try {
            fileStorageService.delete(key);
        }catch (Exception ex){
            throw new RuntimeException("上传文件失败");
        }
        return Response.success().setMsg("文件删除成功");
    }

    @GetMapping("/download")
    @ApiOperation("根据key下载文件")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String key, @RequestParam String fileName) throws IOException {
        if (StringUtil.isBlank(fileName)) {
            fileName = StringUtil.getStringValue(System.currentTimeMillis());
        }
        InputStream inputStream = fileStorageService.getInputStreamByKey(key);
        // 2. 流式写入响应体
        StreamingResponseBody stream = outputStream -> {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            inputStream.close();
        };
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(fileName, PubCommonConst.UTF_8) + "\"")
                .body(stream);
    }

    @PostMapping("/sftp/upload")
    @ApiOperation("上传文件，返回文件地址")
    public Response sftpUpload(@RequestBody Map<String,Object> map) {
        String remoteDirPath =  fileProperties.getPubDirPrefix() + FileConstant.PATH_SEPARATOR  + System.currentTimeMillis();
        SshChannel nasChannelSftp = NasSftpService.getNasChannelSftp();
        ChannelSftp channelSftp = nasChannelSftp.getChannelSftp();
        String localFilePath = StringUtil.getStringValue(map.get("localFilePath"));
        String fileName = FileUtil.getName(Paths.get(localFilePath));
        try(FileInputStream fileInputStream = new FileInputStream(localFilePath)) {
            NasUtil.upLoadInputStream(fileInputStream,remoteDirPath,fileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("SFTP 上传文件失败: " + e.getMessage(), e);
        } finally {
            NasSftpService.returnNasChannelSftp(nasChannelSftp);
        }
        return Response.success().setData(localFilePath).setMsg("FTP文件上传成功");
    }
}
