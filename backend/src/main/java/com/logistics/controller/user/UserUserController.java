package com.logistics.controller.user;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.response.ApiResponse;
import com.logistics.service.user.UserUserService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/user/users")
public class UserUserController {

        @Autowired
        private UserUserService service;

        @GetMapping("check")
        public ResponseEntity<ApiResponse<Boolean>> checkLocked(
                        HttpServletRequest request) {

                Integer userId = (Integer) request.getAttribute("currentUserId");

                return ResponseEntity.ok(service.checkLocked(userId));
        }

}