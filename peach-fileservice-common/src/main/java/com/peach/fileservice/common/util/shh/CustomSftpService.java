package com.peach.fileservice.common.util.shh;


import com.peach.fileservice.common.util.shh.bean.SshChannel;

public class CustomSftpService {

    /**
     * 获取 channelSftp
     *
     * @return channel 对象 Pair<Session, ChannelSftp> channel
     */
    public static SshChannel getChannelSftp() {
        return ChannelSftpUtil.getChannel(ChannelSftpPoolManager.customChannelSftp);
    }

    /**
     * 归还 channelSftp
     *
     * @param channel 对象 Pair<Session, ChannelSftp> channel
     */
    public static void returnChannelSftp(SshChannel channel) {
        ChannelSftpUtil.returnChannel(channel, ChannelSftpPoolManager.customChannelSftp);
    }

    /**
     * 销毁 channelSftp
     *
     * @param channel Pair<Session, ChannelSftp> channel
     */
    public static void destroyChannelSftp(SshChannel channel) {
        ChannelSftpUtil.destroyChannel(channel, ChannelSftpPoolManager.customChannelSftp);
    }

}
