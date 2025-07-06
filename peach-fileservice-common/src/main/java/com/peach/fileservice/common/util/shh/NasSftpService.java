package com.peach.fileservice.common.util.shh;

import com.peach.fileservice.common.util.shh.bean.SshChannel;

public class NasSftpService {

    /**
     * 获取 nasChannelSftp
     *
     * @return channel 对象 Pair<Session, ChannelSftp> channel
     */
    public static SshChannel getNasChannelSftp() {
        return ChannelSftpUtil.getChannel(ChannelSftpPoolManager.nasChannelSftp);
    }

    /**
     * 归还 nasChannelSftp
     *
     * @param channel 对象 Pair<Session, ChannelSftp> channel
     */
    public static void returnNasChannelSftp(SshChannel channel) {
        ChannelSftpUtil.returnChannel(channel, ChannelSftpPoolManager.nasChannelSftp);
    }

    /**
     * 销毁 nasChannelSftp
     *
     * @param channel Pair<Session, ChannelSftp> channel
     */
    public static void destroyNasChannelSftp(SshChannel channel) {
        ChannelSftpUtil.destroyChannel(channel, ChannelSftpPoolManager.nasChannelSftp);
    }

}
