package com.peach.fileservice.controller;

import com.peach.common.mail.EmailSendService;
import com.peach.common.thead.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class TestEmail {


    @Autowired
    private EmailSendService emailService;

    @Autowired
    private ThreadPool threadPool;

    @PostMapping("/test")
    public void test() {
        try {
            emailService.sendSimpleMail("huanhuanshu48@gmail.com", "测试邮件", "测试邮件内容");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/pool")
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
