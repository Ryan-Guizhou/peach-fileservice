package com.peach.fileservice.controller;

import com.peach.common.anno.UserOperLog;
import com.peach.common.constant.EncryptConstant;
import com.peach.common.enums.ModuleEnum;
import com.peach.common.enums.OptTypeEnum;
import com.peach.common.response.Response;
import com.peach.common.util.encrypt.EncryptAbstract;
import com.peach.common.util.encrypt.EncryptFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 04 3月 2025 21:03
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/health")
@Api(tags = "健康检查", value = "健康检查")
public class HealthController {

    @GetMapping("/test/{name}")
    @ApiOperation("健康检查")
    @UserOperLog(moduleCode = ModuleEnum.FILSERVICE,optType = OptTypeEnum.SELECT,
            optContent = "'health check:  params['+#p0.toString()+']'")
    public Response health(@PathVariable("name") String name) {
        log.info("health check");
        return Response.success().setData("ok");
    }

    @GetMapping("/demo")
    @ApiOperation("加解密测试")
    public String demo() throws Exception{
        EncryptAbstract instance = EncryptFactory.getInstance(EncryptConstant.RSA);
        String plaintext = "{\"id\":\"1\",\"name\":\"ryan\"}";
        String encrypted = instance.encrypt(plaintext);
        System.out.println("🔒 加密后: " + encrypted);
        String decrypted = instance.decrypt(encrypted);
        System.out.println("🔓 解密后: " + decrypted);

        instance = EncryptFactory.getInstance(EncryptConstant.DES);
        encrypted = instance.encrypt(plaintext);
        System.out.println("🔒 加密后: " + encrypted);
        decrypted = instance.decrypt(encrypted);
        System.out.println("🔓 解密后: " + decrypted);

        instance = EncryptFactory.getInstance(EncryptConstant.AES);
        encrypted = instance.encrypt(plaintext);
        System.out.println("🔒 加密后: " + encrypted);
        decrypted = instance.decrypt(encrypted);
        System.out.println("🔓 解密后: " + decrypted);
        return "ok";
    }
}
