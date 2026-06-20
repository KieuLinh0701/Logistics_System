package com.logistics.controller.user;

import com.logistics.response.ApiResponse;
import com.logistics.service.user.UserUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/users")
@Tag(name = "User - Profile", description = "Quản lý thông tin và trạng thái tài khoản của người dùng trong hệ thống")
public class UserUserController {

        @Autowired
        private UserUserService service;

        @GetMapping("check")
        public ResponseEntity<ApiResponse<Boolean>> checkLocked(
                        HttpServletRequest request) {

                Integer userId = (Integer) request.getAttribute("currentUserId");

                return ResponseEntity.ok(ApiResponse.success(service.checkLocked(userId)));
        }

}