package com.peach.fileservice.common.util.shh;

import cn.hutool.core.thread.ThreadUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.peach.fileservice.common.util.shh.bean.SshChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/23 18:35
 */
@Slf4j
public class ChannelSftpUtil {

    /**
     * 线程池，用于资源销毁优化
     */
    static ScheduledExecutorService checkExecutor = ThreadUtil.createScheduledExecutor(50);

    /**
     * 创建资源链接，主要适配两种，nas和sftp
     * @param host 主机地址
     * @param userAccount 用户账号
     * @param password 密码
     * @param port 端口号
     * @param timeout 超时时间
     * @param privateKey 私钥地址
     * @param passphrase 加密盐  624511
     * @return
     * @throws JSchException
     */
    public static SshChannel createChannel(String host, String userAccount, String password, int port, int timeout, String privateKey, String passphrase) throws JSchException {
        JSch jsch = new JSch();
        if (StringUtils.isNotEmpty(privateKey)) {
            // 使用密钥验证方式，密钥可以使有口令的密钥，也可以是没有口令的密钥
            if (StringUtils.isNotEmpty(passphrase)) {
                jsch.addIdentity(privateKey, passphrase);
            } else {
                jsch.addIdentity(privateKey);
            }
        }
        Session sshSession = jsch.getSession(userAccount, host, port);
        if (sshSession == null) {
            throw new RuntimeException("session is null");
        }
        if (StringUtils.isNotEmpty(password)) {
            sshSession.setPassword(password);
        }
        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
        sshSession.setConfig(sshConfig);
        sshSession.setTimeout(timeout);
        sshSession.connect();
        ChannelSftp channelSftp = (ChannelSftp) sshSession.openChannel("sftp");
        if (channelSftp == null) {
            throw new RuntimeException("Could not open sftp channel");
        }
        channelSftp.connect(timeout);
        return new SshChannel(sshSession, channelSftp);
    }


    /**
     * 资源销毁
     * @param channel 资源
     * @param pool 资源池子
     */
    public static void destroyChannel(SshChannel channel, GenericObjectPool<SshChannel> pool) {
        try {
            if (channel != null) {
                channel.destroy();
                channel.setIsUsed(false);
            }
            pool.invalidateObject(channel);
        } catch (Exception e) {
            log.debug("destroyChannel error" + e.getMessage(),e);
        }
    }
    /**
     * 获取资源
     * @param pool 资源池子
     */
    public static SshChannel getChannel(GenericObjectPool<SshChannel> pool) {
        SshChannel sshChannel = null;
        try {
            sshChannel = pool.borrowObject();
            sshChannel.setIsUsed(true);
        } catch (Exception e) {
            log.error("getSshChannel error" + e.getMessage(),e);
        } finally {
            //优化自动回收机制， SshChannel 借出两分钟后自动回收
            SshChannel channel = sshChannel;
            checkExecutor.schedule(() -> {
                //五秒未归还 则开启另一个定时调度 自动归还
                if (channel != null && channel.getIsUsed()) {
                    checkExecutor.schedule(() -> {
                        try {
                            if (channel.getIsUsed()) {
                                channel.setIsUsed(false);
                                pool.returnObject(channel);
                            }
                        } catch (Exception e) {
                            log.error("no need to pay this exception：" + e.getMessage(),e);
                        }
                    }, 10, TimeUnit.MINUTES);
                }
            }, 10, TimeUnit.SECONDS);
        }
        if (sshChannel == null) {
            throw new RuntimeException("please check the ssh connection conf!");
        }
        return sshChannel;
    }

    /**
     * 归还资源
     * @param channel 资源
     * @param pool 资源池子
     */
    public static void returnChannel(SshChannel channel, GenericObjectPool<SshChannel> pool) {
        try {
            channel.setIsUsed(false);
            if (channel.isValid()) {
                pool.returnObject(channel);
            }
        } catch (Exception e) {
            log.error("returnChannel error" + e.getMessage(),e);
        }
    }
}
