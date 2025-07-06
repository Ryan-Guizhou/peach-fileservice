package com.peach.fileservice.common.util;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.common.util.PeachCollectionUtil;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.constant.FileConstant;
import com.peach.fileservice.common.util.shh.bean.SshChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/7/4 10:07
 */
@Slf4j
public class BaseSftpUtil {

    /**
     * 创建重试器 / Creating a retryer
     * eg: boolean result = BaseSftpUtil.SFTP_RETRYER.call(() -> yourSftpOperation());
     */
    protected static Retryer<Boolean> SFTP_RETRYER = RetryerBuilder.<Boolean>newBuilder()
            .retryIfResult(result -> result == null || !result) // 失败条件：返回false或null时重试
            .retryIfException() // 任何异常都重试
            .withStopStrategy(StopStrategies.stopAfterAttempt(3)) // 最多重试3次
            .withWaitStrategy(WaitStrategies.fixedWait(2, TimeUnit.SECONDS)) // 每次重试间隔2秒
            .build();

    /**
     * 合并分片，多次重试 / Retrying multiple times
     * eg:boolean success = BaseSftpUtil.mergeChunkByChannelWithRetry("/tmp/chunks/", ".dat", "final.txt", channel);
     * @param chunkPath 分片路径 / Chunk path
     * @param chunkSuffix 分片后缀 / Chunk suffix
     * @param finalName 最终文件名 / Final file name
     * @param channel sshChannel 连接 / sshChannel connection
     * @return
     */
    public static boolean mergeChunkByChannelWithRetry(String chunkPath, String chunkSuffix, String finalName, SshChannel channel){
        try {
            return SFTP_RETRYER.call(()-> mergeChunkByChannelOnce(chunkPath,chunkSuffix,finalName,channel));
        }catch (Exception e){
            log.error("[mergeChunkByChannelWithRetry] 合并分片重试多次仍失败: {}", e.getMessage(), e);
            return Boolean.FALSE;
        }
    }

    /**
     * 合并分片，不重试
     * eg: boolean success = BaseSftpUtil.mergeChunkByChannelOnce("/tmp/chunks/", ".dat", "final.txt", channel);
     * @param chunkPath 分片路径 / Chunk path
     * @param chunkSuffix 分片后缀 / Chunk suffix
     * @param finalName 最终文件名 / Final file name
     * @param channel sshChannel 连接 / sshChannel connection
     * @return
     */
    private static boolean mergeChunkByChannelOnce(String chunkPath, String chunkSuffix, String finalName, SshChannel channel) throws Exception {
        if (channel == null || StringUtils.isBlank(chunkPath) || StringUtils.isBlank(finalName)) {
            log.error("参数非法，channel/chunkPath/finalName 不能为空");
            return Boolean.FALSE;
        }
        ChannelSftp sftp = channel.getChannelSftp();
        String finalFilePath = joinPath(chunkPath,finalName);
        // 如果目标文件已存在，先删除
        if (checkIsExistByChannel(finalFilePath, channel)) {
            deleteFile(finalFilePath, sftp);
        }
        String suffix = StringUtils.isBlank(chunkSuffix) ? ".dat" : chunkSuffix;
        log.info("[mergeChunkByChannelOnce] 检查分片后缀: [{}]", suffix);
        // 获取所有分片文件名
        Vector<ChannelSftp.LsEntry> files;
        try {
            files = sftp.ls(chunkPath);
        } catch (SftpException e) {
            log.error("ls 目录失败: [{}]", chunkPath, e);
            return Boolean.FALSE;
        }
        List<String> chunkNameList = new ArrayList<>();
        for (ChannelSftp.LsEntry file : files) {
            if (!file.getAttrs().isDir() && file.getFilename().endsWith(suffix)) {
                chunkNameList.add(file.getFilename());
            }
        }
        if (PeachCollectionUtil.isEmpty(chunkNameList)) {
            log.warn("[mergeChunkByChannelOnce] 未找到任何分片文件，目录: [{}]", chunkPath);
            return Boolean.FALSE;
        }
        // 按分片序号排序
        chunkNameList.sort((o1, o2) -> {
            Integer i1 = Integer.valueOf(o1.substring(o1.lastIndexOf("-") + 1, o1.lastIndexOf(".")));
            Integer i2 = Integer.valueOf(o2.substring(o2.lastIndexOf("-") + 1, o2.lastIndexOf(".")));
            return i1.compareTo(i2);
        });
        log.info("[mergeChunkByChannelOnce] 分片排序后: [{}]", JSON.toJSON(chunkNameList));
        // 依次合并分片
        for (String chunkName : chunkNameList) {
            String chunkFilePath = joinPath(chunkPath, chunkName);
            try (InputStream inputStream = sftp.get(chunkFilePath)) {
                sftp.put(inputStream, finalFilePath, ChannelSftp.APPEND);
                log.info("[mergeChunkByChannelOnce] 已追加分片: [{}] 到 [{}]", chunkName, finalName);
                // 合并后删除分片
                deleteFile(chunkFilePath, sftp);
            } catch (Exception e) {
                log.error("[mergeChunkByChannelOnce] 合并分片失败: [{}] -> [{}], error: [{}]", chunkName, finalName, e.getMessage(), e);
                throw e;
            }
        }
        log.info("[mergeChunkByChannelOnce] 合并完成: [{}]", finalFilePath);
        // 校验合并后文件是否存在
        boolean exist = checkIsExistByChannel(finalFilePath, channel);
        if (!exist) {
            log.error("[mergeChunkByChannelOnce] 合并后文件不存在: [{}]", finalFilePath);
        }
        return exist;
    }

    /**
     * 合并文件并上传到指定的目录 / Merging files and uploading to the specified directory
     * eg:BaseSftpUtil.mergeChunkByChannelToTargetPath("/tmp/chunks/", "/tmp/target/", ".dat", "final.txt", channel, response -> "ok");
     * @param chunkPath 分片路径 / Chunk path
     * @param targetPath 目标路径 / Target path
     * @param chunkSuffix 分片后缀 / Chunk suffix
     * @param finalName 最终文件名 / Final file name
     * @param channel sshChannel 连接 / sshChannel connection
     * @return
     */
    public static boolean mergeChunkByChannelToTargetPath(String chunkPath, String targetPath, String chunkSuffix, String finalName, SshChannel channel, Function<Response, String> dealwithChunkInfoFun) {
        ChannelSftp sftp = channel.getChannelSftp();
        String key = joinPath(targetPath,finalName);
        try {
            if (checkIsExist(key, sftp)) {
                deleteFile(key,sftp);
            }
            log.info("===========Checking chunk file suffix===========");
            Vector<ChannelSftp.LsEntry> files = sftp.ls(chunkPath);
            List<String> chunkNameList = new ArrayList<>();
            chunkSuffix = StringUtil.isBlank(chunkSuffix) ? ".dat" : chunkSuffix;
            for (ChannelSftp.LsEntry file : files) {
                if (!file.getAttrs().isDir() && file.getFilename().endsWith(chunkSuffix)) {
                    chunkNameList.add(file.getFilename());
                }
            }
            if (PeachCollectionUtil.isEmpty(chunkNameList)) {
                return Boolean.FALSE;
            }

            log.info("===========Sorting chunks===========");
            chunkNameList.sort((o1, o2) -> {
                Integer i1 = Integer.valueOf(o1.substring(o1.lastIndexOf("-") + 1));
                Integer i2 = Integer.valueOf(o2.substring(o2.lastIndexOf("-") + 1));
                return i1.compareTo(i2);
            });

            log.info("Starting merge,finalName:[{}] Chunk count:[{}]" ,finalName, chunkNameList.size());
            for (String chunkName : chunkNameList) {
                try (InputStream inputStream = sftp.get(joinPath(chunkPath, chunkName));
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    byte[] bytes = outputStream.toByteArray();
                    String targetPathWithName = joinPath(targetPath, finalName);
                    log.info("targetPath:[{}] ,finalName:[{}],targetPathWithName:[{}]" ,targetPath,finalName, targetPathWithName);
                    boolean isSuccess = createDirectory(targetPath, sftp);
                    if (isSuccess) {
                        sftp.put(new ByteArrayInputStream(bytes), targetPathWithName, ChannelSftp.APPEND);
                    }
                    try {
                        SftpATTRS sftpATTRS = sftp.lstat(targetPathWithName);
                        log.info("finalName:[{}],chunkName:[{}],fileSize:[{}]" ,finalName,chunkName, sftpATTRS.getSize());
                    } catch (SftpException e) {
                        log.error("File does not exist!"+e.getMessage(), e);
                    }
                    String chunkPathWithName = joinPath(chunkPath, chunkName);
                    boolean isDeleted = deleteFile(chunkPathWithName, sftp);
                    log.info("chunkPath:[{}],chunkName:[{}],chunkPathWithName:[{}],Delete chunk:[{}]", chunkPath,chunkName,chunkPathWithName, isDeleted);
                    Response chunkInfo = Response.success();
                    Map<String,Object> result = new HashMap<>();
                    result.put("chunkComplete",chunkNameList.indexOf(chunkName) + 1);
                    if (chunkNameList.indexOf(chunkName) == chunkNameList.size() - 1) {
                        result.put("complete",Boolean.TRUE);
                    } else {
                        result.put("complete",Boolean.FALSE);
                    }
                    chunkInfo.setData(chunkInfo);
                    dealwithChunkInfoFun.apply(chunkInfo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            log.info("Merge completed,finalName:[{}] Chunk count:[{}]", finalName, chunkNameList.size());
        } catch (Exception e) {
            log.error("Merge failed"+e.getMessage(), e);
        }
        return checkIsExist(key,sftp);
    }


    /**
     * 上传文件流 / Upload file stream
     * eg: boolean success = BaseSftpUtil.uploadInputStreamByChannel(in, "/remote/path/", "file.txt", channel);
     * @param inputStream 文件流 / file stream
     * @param uploadPath 目标路径 / target path
     * @param uploadName 文件名 / file name
     * @param sshChannel  sshChannel 连接
     * @return
     */
    public static boolean uploadInputStreamByChannel(InputStream inputStream, String uploadPath, String uploadName, SshChannel sshChannel) {
        if (sshChannel == null) {
            return Boolean.FALSE;
        }
        Long remoteNasDelayTime = 1L;
        Long remoteNasWaitMaxTime = 1L;
        ChannelSftp channelSftp = sshChannel.getChannelSftp();
        try {
            log.info("uploadPath:[{}],uploadName:[{}]" ,uploadPath, uploadName);
            String[] dirs = uploadPath.split(FileConstant.PATH_SEPARATOR);
            try {
                channelSftp.cd(FileConstant.PATH_SEPARATOR);
            } catch (Exception e) {
                log.info("First cd root directory failed;uploadName:[{}]" ,uploadName);
                try {
                    Thread.sleep(remoteNasDelayTime);
                    channelSftp.cd(FileConstant.PATH_SEPARATOR);
                } catch (Exception ex) {
                    log.info("Directory cd / failed ;uploadName: [{}]" , uploadName);
                    return false;
                }
            }
            boolean mkdirSuccess = true;
            for (String str : dirs) {
                if (StringUtils.isBlank(str)) {
                    continue;
                }
                try {
                    channelSftp.cd(str);
                } catch (SftpException e) {
                    log.info("First directory cd:[{}] failed;uploadName:[{}]",str ,uploadName,e.getMessage());
                    Date initTime = new Date();
                    try {
                        while (!checkIsDirExist(str, channelSftp)) {
                            long timeOut = DateUtil.between(initTime, new Date(), DateUnit.SECOND);
                            if (timeOut >= remoteNasWaitMaxTime) {
                                throw new RuntimeException("wait time out...");
                            }
                            channelSftp.mkdir(str);
                            Thread.sleep(remoteNasDelayTime);
                        }
                    } catch (Exception ex) {
                        log.info("Create directory [{}] failed;uploadName:[{}]" ,str ,uploadName);
                        mkdirSuccess = Boolean.FALSE;
                        break;
                    }
                    channelSftp.cd(str);
                }
            }
            log.info("Create directory [{}] and switch directory status is:[{}]" ,uploadPath, mkdirSuccess);
            boolean isSuccess = createDirectory(uploadPath, channelSftp);
            if (isSuccess) {
                try (InputStream input = inputStream){
                    channelSftp.put(input, uploadPath+FileConstant.PATH_SEPARATOR+uploadName);
                    log.info("uploadName:" + uploadName + "_File write success.");
                    channelSftp.setFilenameEncoding(PubCommonConst.UTF_8);
                    SftpATTRS sftpATTRS = channelSftp.lstat(uploadName);
                    log.info("uploadName:" + uploadName + " ,size:" + sftpATTRS.getSize());
                }catch (SftpException e){
                    log.error("File does not exist!;uploadName:" + uploadName, e);
                }
            }
        }catch (Exception e){
            log.error("File upload exception;uploadName:[{}]" , uploadName);
        }
        return Boolean.TRUE;
    }

    /**
     * 获取文件流 / Get file stream
     * @param key 文件路径
     * @param sshChannel sshChannel 连接
     * @return
     * @throws SftpException
     */
    public static InputStream getInputStreamByChannel(String key, SshChannel sshChannel) throws SftpException {
        if (sshChannel == null) {
            return null;
        }
        ChannelSftp channelSftp = sshChannel.getChannelSftp();
        return channelSftp.get(key);
    }

    /**
     * 检查文件是否存在
     * @param key 文件路径
     * @param sshChannel sshChannel 连接
     * @return
     */
    public static boolean checkIsExistByChannel(String key, SshChannel sshChannel) {
        if (sshChannel == null) {
            return Boolean.FALSE;
        }
        ChannelSftp channelSftp = sshChannel.getChannelSftp();
        return checkIsExist(key, channelSftp);
    }


    /**
     * 删除文件或者文件夹 / Delete file
     * @param folderPath 文件路径 / file path
     * @param sftpChannel sshChannel 连接 / sshChannel connection
     * @return
     */
    public static boolean deleteByChannel(String folderPath, SshChannel sftpChannel) {
        boolean isDeleted = Boolean.TRUE;
        // 判断路径是否合法 是否可删除
        if ("/".equals(folderPath) || "//".equals(folderPath) || "\\".equals(folderPath) || "\\\\".equals(folderPath)
                || ".".equals(folderPath) || "..".equals(folderPath) || "".equals(folderPath)) {
            throw new RuntimeException("File folder path cannot be / or ..");
        }
        try {
            ChannelSftp channelSftp = sftpChannel.getChannelSftp();
            SftpATTRS stat = channelSftp.stat(folderPath);
            if (stat != null && stat.isDir()) {
                deleteDirectory(folderPath, channelSftp);
            }else {
                isDeleted = deleteFile(folderPath,channelSftp);
            }
        } catch (SftpException e) {
            isDeleted = Boolean.FALSE;
            log.error("deleteByChannel error"+e.getMessage(), e);
        }
        return isDeleted;
    }


    /**
     * 递归算法 通过"先清空内容，再删除目录"的方式安全地删除整个目录树 / Delete the entire directory tree using "clear content, then delete directory"
     * 删除文件夹 / Delete folder
     * @param directoryPath 文件夹路径 / folder path
     * @param channelSftp sshChannel 连接 / sshChannel connection
     * @throws SftpException sshChannel 异常 / sshChannel exception
     */
    public static void deleteDirectory(String directoryPath,ChannelSftp channelSftp) throws SftpException {
        channelSftp.cd(directoryPath);
        Vector lsResult = channelSftp.ls(".");
        for (Object item : lsResult) {
            ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) item;
            String filename = lsEntry.getFilename();
            log.info("filename:[{}]",filename);
            if (".".equals(filename) || "..".equals(filename)) {
                continue;
            }
            if (lsEntry.getAttrs().isDir()) {
                deleteDirectory(joinPath(directoryPath,filename),channelSftp);
            } else {
                channelSftp.rm(joinPath(directoryPath,filename));
            }
        }
        channelSftp.cd("..");
        channelSftp.rm(directoryPath);
    }

    /**
     * 删除文件 / Delete file
     * @param filePath 文件路径 / file path
     * @param channelSftp sshChannel 连接 / sshChannel connection
     * @return
     */
    public static boolean deleteFile(String filePath,ChannelSftp channelSftp) {
        boolean isDeleted = Boolean.FALSE;
        try {
            channelSftp.rm(filePath);
            try {
                channelSftp.stat(filePath);
            }catch (Exception e){
                isDeleted = Boolean.TRUE;
            }
            cleanupEmptyDirs(channelSftp,filePath);
        }catch (SftpException e){
            log.error("sshChannel delete file exception"+e.getMessage(),e);
        }
        return isDeleted;
    }


    /**
     * 创建文件夹 / Create folder
     * @param directory 文件夹路径 / folder path
     * @param channelSftp sshChannel 连接 / sshChannel connection
     * @return 是否创建成功 / Whether it was created successfully
     */
    public static boolean createDirectory(String directory,ChannelSftp channelSftp){
        try {
            if (checkIsDirExist(directory,channelSftp)){
                channelSftp.cd(directory);
                return Boolean.TRUE;
            }
            String[] pathArray = directory.split(FileConstant.PATH_SEPARATOR);
            StringBuffer filePath = new StringBuffer(FileConstant.PATH_SEPARATOR);
            for (String path : pathArray) {
                if ("".equals(path) || null == path){
                    continue;
                }
                filePath.append(path).append(FileConstant.PATH_SEPARATOR);
                if (checkIsDirExist(StringUtil.getStringValue(filePath),channelSftp)){
                    channelSftp.cd(path);
                }else {
                    channelSftp.mkdir(path);
                    channelSftp.cd(path);
                }
            }
            return Boolean.TRUE;
        }catch (SftpException e){
            log.error("sshChannel create directory exception"+e.getMessage(),e);
            return Boolean.FALSE;
        }
    }

    /**
     * 判断文件夹 是否存在 / Determine if the folder exists
     * @param directory 文件夹路径 / folder path
     * @param channelSftp sshChannel 连接 / sshChannel connection
     * @return 是否存在 / Whether it exists
     */
    public static boolean checkIsDirExist(String directory, ChannelSftp channelSftp) {
        long startTime = System.currentTimeMillis();
        boolean flag = Boolean.TRUE;
        try {
            channelSftp.lstat(directory);
        }catch (SftpException e){
            flag = Boolean.FALSE;
        }
        long endTime = System.currentTimeMillis();
        log.info("checkIsDirExist key:[{}], costTime:[{}]",directory,(endTime-startTime));
        return flag;
    }


    /**
     * 判断文件/文件夹 是否存在 / Determine if the file/folder exists
     * @param key 文件路径/文件夹路径 / file path/folder path
     * @param channelSftp sshChannel 连接 / sshChannel connection
     * @return 是否存在 / Whether it exists
     */
    public static boolean checkIsExist(String key, ChannelSftp channelSftp) {
        long startTime = System.currentTimeMillis();
        boolean flag = Boolean.TRUE;
        try {
            channelSftp.stat(key);
        }catch (SftpException e){
            flag = Boolean.FALSE;
        }
        long endTime = System.currentTimeMillis();
        log.info("checkExistByChannelSftp key:[{}] , time:[{}]",key,(endTime-startTime));
        return flag;
    }

    /**
     * 标准化路径 / Standardize path
     * @param dir 路径 / path
     * @return
     */
    private static String normalizeDir(String dir) {
        if (StringUtils.isBlank(dir)) {
            return FileConstant.PATH_SEPARATOR;
        }
        dir = dir.replaceAll(FileConstant.SEPARATOR_REG, FileConstant.PATH_SEPARATOR);
        if (!dir.endsWith(FileConstant.PATH_SEPARATOR)) {
            dir += FileConstant.PATH_SEPARATOR;
        }
        return dir;
    }

    /**
     * 拼接路径 / Join path
     * @param dir 路径 / path
     * @param fileName 文件名 / file name
     * @return
     */
    private static String joinPath(String dir, String fileName) {
        if (StringUtils.isBlank(dir)){
            return fileName;
        }
        if (StringUtils.isBlank(fileName)){
            return normalizeDir(dir);
        }
        if (fileName.startsWith(FileConstant.PATH_SEPARATOR)) {
            fileName = fileName.substring(1);
        }
        return normalizeDir(dir) + fileName;
    }

    /**
     * 清除多余的空目录文件夹
     * @param sftp
     * @param filePath
     */
    private static void cleanupEmptyDirs(ChannelSftp sftp, String filePath) {
        try {
            String parent = new File(filePath).getParent();
            parent = normalizePath(parent);
            while (parent != null && !FileConstant.PATH_SEPARATOR.equals(parent)) {
                if (isDirEmpty(sftp, parent)) {
                    sftp.rmdir(parent);
                    parent = new File(parent).getParent();
                } else {
                    break; // 遇到非空目录就停止
                }
            }
        } catch (Exception e) {
            log.warn("cleanup empty dir field"+e.getMessage(), e);
        }
    }

    /**
     * 判断是否为空的文件夹
     * @param sftp
     * @param dirPath
     * @return
     */
    private static boolean isDirEmpty(ChannelSftp sftp, String dirPath) {
        try {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(dirPath);
            // 排除 . 和 ..
            return entries.stream()
                    .filter(e -> !e.getFilename().equals(".") && !e.getFilename().equals(".."))
                    .count() == 0;
        } catch (SftpException e) {
            return Boolean.FALSE;
        }
    }

    private static String normalizePath(String key){
        if (StringUtils.isBlank(key)) {
            return key;
        }
        String result = key.trim().replace("\\", "/")        // 将所有反斜杠替换为正斜杠
                .replaceAll("/\\./", "/")
                .replaceAll("/{2,}", "/");          // 多个 / 替换为一个

        log.debug("[normalizePath] input: [{}], normalized: [{}]", key, result);
        return result;
    }

}
