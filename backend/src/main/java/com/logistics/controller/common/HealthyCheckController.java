package com.logistics.controller.common;

import com.logistics.request.common.user.UpdateEmailRequest;
import com.logistics.request.common.user.UpdatePasswordRequest;
import com.logistics.request.common.user.UpdateProfileRequest;
import com.logistics.request.common.user.VerifyEmailUpdateOTPRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.UserPublicService;
import com.logistics.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/health")
public class HealthyCheckController {

    @GetMapping
    public ResponseEntity<Void> healthCheck() {
        log.info("Health check ping received");
        return ResponseEntity.ok().build();
    }
}