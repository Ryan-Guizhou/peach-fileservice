package com.peach.fileservice.common.util.shh;

import com.peach.fileservice.common.util.shh.bean.SshChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/6/26 22:26
 */
@Slf4j
@Indexed
@Component
public class NasSftpFactory extends BasePooledObjectFactory<SshChannel> {

    /**
     *  sftp 地址
     */
    @Value("${sftp.nas.host:47.121.112.66}")
    private String host;

    /**
     *  sftp 用户名
     */
    @Value("${sftp.nas.account:root}")
    private String account;

    /**
     * 密码
     */
    @Value("${sftp.nas.password:Xs...520149}")
    private String password;

    /**
     * 私钥 私钥地址
     */
    @Value("${sftp.nas.privateKey:C:/Users/Administrator/.ssh/id_rsa-pem}")
    private String privateKey;

    /**
     *
     */
    @Value("${sftp.nas.passphrase:624511}")
    private String passphrase;

    /**
     * 端口号
     */
    @Value("${sftp.nas.port:22}")
    private int port;

    /**
     * 连接超时时间
     */
    @Value("${sftp.custom.timeout:1000}")
    private int timeout;

    @Override
    public SshChannel create() throws Exception {
        return ChannelSftpUtil.createChannel(host,account,password,port,timeout,privateKey,passphrase);
    }

    @Override
    public PooledObject<SshChannel> wrap(SshChannel sshChannel) {
        return new DefaultPooledObject<>(sshChannel);
    }

    @Override
    public PooledObject<SshChannel> makeObject() throws Exception {
        SshChannel channel = create();
        return wrap(channel);
    }

    @Override
    public void destroyObject(PooledObject<SshChannel> pooledObject, DestroyMode destroyMode) throws Exception {
        SshChannel channel = pooledObject.getObject();
        try {
            ChannelSftpUtil.destroyChannel(channel, ChannelSftpPoolManager.nasChannelSftp);
        } catch (Exception e) {
            log.error("destroyObject error" + e.getMessage(),e);
        }
    }

    @Override
    public boolean validateObject(PooledObject<SshChannel> pooledObject) {
        SshChannel channel = pooledObject.getObject();
        if (channel.isValid()) {
            // 执行销毁前的清理操作，例如关闭连接等
            return Boolean.TRUE;
        }

        try {
            destroyObject(pooledObject, DestroyMode.NORMAL);
        } catch (Exception e) {
            log.error("validateObject error" + e.getMessage(),e);
        }
        return Boolean.FALSE;
    }
}
