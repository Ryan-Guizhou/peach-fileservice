package com.peach.fileservice.common.util.shh.bean;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/26 22:22
 */
@Data
@Slf4j
public class SshChannel implements Serializable {

    private static final long serialVersionUID = 1L;

    private Session sshSession;

    private ChannelSftp channelSftp;

    private Boolean isUsed = Boolean.FALSE;

    public SshChannel(Session sshSession, ChannelSftp channelSftp) {
        this.sshSession = sshSession;
        this.channelSftp = channelSftp;
    }

    public SshChannel() {

    }

    /**
     * 资源销毁
     */
    public void destroy() {
        try{
            this.channelSftp.disconnect();
        }catch(Exception e){
            log.error("destory channelSftp error"+e.getMessage(), e);
        }
        try {
            this.sshSession.disconnect();
        }catch(Exception e){
            log.error("destory session error"+e.getMessage(), e);
        }
    }

    /**
     * 判断资源是否有效
     * @return
     */
    public boolean isValid() {
        return channelSftp != null && sshSession != null;
    }
}
