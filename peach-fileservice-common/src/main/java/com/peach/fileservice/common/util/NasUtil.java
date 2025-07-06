package com.peach.fileservice.common.util;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.peach.fileservice.common.util.shh.NasSftpService;
import com.peach.fileservice.common.util.shh.bean.SshChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Vector;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/7/6 13:17
 */
@Slf4j
public class NasUtil extends BaseSftpUtil{

    /**
     * 拷贝文件 或者 文件夹 / Copy files or folders
     * @param sourceDir 源文件路径 / source file path
     * @param targetDir 目标文件路径 / target file path
     */
    public static void copyDir(String sourceDir,String targetDir) {
        SshChannel nasChannelSftp = NasSftpService.getNasChannelSftp();
        try {
            Session shhSession = nasChannelSftp.getSshSession();
            ChannelExec exec = (ChannelExec)shhSession.openChannel("exec");
            // 构建命令
            String copyCommand = String.format("cp -R %s %s", sourceDir, targetDir);
            log.info("copyCommand:{}" + copyCommand);
            exec.setCommand(copyCommand);
            exec.setErrStream(System.err);
            // 休眠 1 秒是为了确保命令已经被远程 shell 正确接收和准备好，避免出现“流未就绪”或“命令未生效”的偶发问题
            Thread.sleep(1000);
            try (InputStream inputStream = exec.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                // 获取命令执行结果 60s的超时时间
                exec.connect(60 * 1000);
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("copyDir command execution result:[{}]" ,line);
                }
            }

        }catch (Exception e) {
            log.error("File copy exception"+e.getMessage(), e);
        }finally {
            NasSftpService.returnNasChannelSftp(nasChannelSftp);
        }

    }

    public static void downDir(String targetPath, String localPath) {
        SshChannel channel = NasSftpService.getNasChannelSftp();
        String localDir = localPath.endsWith(File.separator) ? localPath : localPath + File.separator;
        try {
            ChannelSftp sftp = channel.getChannelSftp();
            Vector<ChannelSftp.LsEntry> files = sftp.ls(targetPath);
            for (ChannelSftp.LsEntry file : files) {
                String fileName = file.getFilename();
                try (InputStream inputStream = sftp.get(targetPath + fileName)) {
                    FileUtil.writeFromStream(inputStream, localDir + fileName);
                } catch (Exception e) {
                    log.error("File download exception!", e);
                }
            }
        } catch (Exception e) {
            log.error("File download exception!", e);
        } finally {
            NasSftpService.returnNasChannelSftp(channel);
        }
    }

    public static boolean upLoadInputStream(InputStream input, String targetPath, String fileName) {
        SshChannel channel = NasSftpService.getNasChannelSftp();
        try {
            return uploadInputStreamByChannel(input, targetPath, fileName, channel);
        } catch (Exception e) {
            log.error("File upload exception!", e);
        } finally {
            NasSftpService.returnNasChannelSftp(channel);
        }
        return false;
    }

    public static InputStream getInputStream(String targetPath) {
        SshChannel channel = NasSftpService.getNasChannelSftp();
        try {
            InputStream inputStream = getInputStreamByChannel(targetPath, channel);
            new Thread(() -> {
                try {
                    releaseChannel(inputStream, channel);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
            return inputStream;
        } catch (SftpException e) {
            log.error("getInputStream!", e);
            return null;
        }
    }

    public static void releaseChannel(InputStream inputStream, SshChannel channel) throws Exception {
        Field closedField = inputStream.getClass().getDeclaredField("closed");
        closedField.setAccessible(true);
        boolean closed = (boolean) closedField.get(inputStream);
        if (closed) {
            NasSftpService.returnNasChannelSftp(channel);
        } else {
            Date startTime = new Date();
            while (!closed) {
                long timeOut = DateUtil.between(startTime, new Date(), DateUnit.HOUR);
                if (timeOut >= 2) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.error("releaseChannel field"+e.getMessage(),e);
                }
                closed = (boolean) closedField.get(inputStream);
            }
            NasSftpService.returnNasChannelSftp(channel);
        }
    }


    /**
     * 检查文件或者文件夹为空 / Check if the file or folder is empty
     * @param key 文件路径 / file path
     * @return
     */
    public static boolean exist(String key) {
        SshChannel nasChannelSftp = NasSftpService.getNasChannelSftp();
        try {
            return checkIsExistByChannel(key,nasChannelSftp);
        }finally {
            NasSftpService.returnNasChannelSftp(nasChannelSftp);
        }
    }

    /**
     * 删除文件夹 / data/nas/
     * @param folderPath 文件夹路径 / data/nas/
     * @return
     */
    public static boolean delete(String folderPath){
        SshChannel nasChannelSftp = NasSftpService.getNasChannelSftp();
        try {
            return deleteByChannel(folderPath,nasChannelSftp);
        }finally {
            NasSftpService.returnNasChannelSftp(nasChannelSftp);
        }
    }
}
