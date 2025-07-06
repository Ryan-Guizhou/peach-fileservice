package com.peach.fileservice.common.util.shh;

import com.peach.common.util.StringUtil;
import com.peach.fileservice.common.enums.SftpEnum;
import com.peach.fileservice.common.util.shh.bean.SshChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description // TODO
 * @CreateTime 2025/6/23 17:54
 */
@Slf4j
@Component
public class ChannelSftpPoolManager {

    @Value("${sftp.pool.maxTotal: 8}")
    private Integer maxTotal;

    @Value("${sftp.pool.minIdle: 1}")
    private Integer minIdle;

    @Value("${sftp.pool.maxIdle: 4}")
    private Integer maxIdle;

    @Value("${sftp.pool.testWhileIdle: true}")
    private boolean testWhileIdle;

    @Value("${sftp.pool.testOnBorrow: true}")
    private boolean testOnBorrow;

    @Value("${sftp.pool.numTestsPerEvictionRun: 8}")
    private Integer numTestsPerEvictionRun;

    @Value("${sftp.pool.type: NAS}")
    private String type;

    @Value("${sftp.pool.channelSftpDelay: 1000}")
    private Integer channelSftpDelay;

    @Value("${sftp.pool.channelSftpCheckDelay: 3000}")
    private Integer channelSftpCheckDelay;

    public static GenericObjectPool<SshChannel> nasChannelSftp;

    public static GenericObjectPool<SshChannel> customChannelSftp;

    @Resource
    private NasSftpFactory nasSftpFactory;

    @Resource
    private CustomSftpFactory customSftpFactory;

    @Bean("objectPoolConfig")
    public GenericObjectPoolConfig objectPoolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        // 设置连接池的最大连接数
        config.setMaxTotal(maxTotal);

        // 设置连接池的最小空闲连接数
        config.setMinIdle(minIdle);

        // 设置连接池的最大空闲连接数
        config.setMaxIdle(maxIdle);

        // 连接的空闲状态检测
        config.setTestWhileIdle(testWhileIdle);

        // 检测连接有效性
        config.setTestOnBorrow(testOnBorrow);

        // 每次驱逐运行时要测试的连接数量
        config.setNumTestsPerEvictionRun(numTestsPerEvictionRun);

        customChannelSftp = new GenericObjectPool<>(customSftpFactory,config);
        if (StringUtil.isNotBlank(type) && type.equalsIgnoreCase(SftpEnum.NAS.name())) {
            nasChannelSftp = new GenericObjectPool(nasSftpFactory, config);
        }
        return config;
    }
}
