package com.logistics.controller.common;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Kiểm tra trạng thái hoạt động của hệ thống")
public class HealthyCheckController {

    @GetMapping
    public ResponseEntity<Void> healthCheck() {
        log.info("Health check ping received");
        return ResponseEntity.ok().build();
    }
}