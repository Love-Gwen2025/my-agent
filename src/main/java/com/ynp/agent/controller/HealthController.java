package com.ynp.agent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口。
 */
@RestController
public class HealthController {

    /**
     * 1. 返回健康状态。
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> body = new HashMap<>();
        /* 1. 返回固定健康状态 */
        body.put("status", "ok");
        return ResponseEntity.ok(body);
    }
}
