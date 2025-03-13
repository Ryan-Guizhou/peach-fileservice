package com.peach.fileservice.controller;

import com.peach.common.util.encrypt.EncryptAbstract;
import com.peach.common.util.encrypt.EncryptFactory;
import com.peach.common.constant.EncryptConstant;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/3/13 13:38
 */
@RequestMapping("/demo")
@RestController
public class DemoController {

    @RequestMapping("")
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
