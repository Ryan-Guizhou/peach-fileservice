package com.peach.fileservice.controller;

import com.peach.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.GetMapping;
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
public class HealthController {

    @GetMapping
    public Response health() {
        log.info("health check");
        return Response.success().setMsg("ok");
    }
}
