package com.peach.fileservice.controller;

import com.peach.common.mail.EmailSendService;
import com.peach.common.thead.ThreadPool;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/12 18:51
 */
@Slf4j
@RestController
@RequestMapping("/mail")
@Api(tags = "邮件测试", value = "邮件测试")
public class TestEmail {


    @Resource
    private EmailSendService emailService;

    @Resource
    private ThreadPool threadPool;

    @PostMapping("/test")
    @ApiOperation(value = "简单邮件发送")
    public void test() {
        try {
            emailService.sendSimpleMail("huanhuanshu48@gmail.com", "测试邮件", "测试邮件内容");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/pool")
    @ApiOperation(value = "线程池测试")
    public void pool() {
        ExecutorService executorService = threadPool.newCachedThreadPool(TestEmail.class);
        AtomicInteger a = new AtomicInteger(0);
        for (int i = 0; i < 500; i++) {
            executorService.execute(() ->{
                try {
                    log.info("线程池执行,[{}]",a.getAndIncrement());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

}
