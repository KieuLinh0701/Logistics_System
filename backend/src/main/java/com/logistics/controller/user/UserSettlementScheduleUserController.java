package com.logistics.controller.user;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.BankAccountDto;
import com.logistics.dto.UserSettlementScheduleDto;
import com.logistics.request.user.bankAccount.BankAccountRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.UserSettlementScheduleUserService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/user/user-settlement-batchs")
public class UserSettlementScheduleUserController {

        @Autowired
        private UserSettlementScheduleUserService service;

        @GetMapping()
        public ResponseEntity<ApiResponse<UserSettlementScheduleDto>> getUserSchedule(
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                ApiResponse<UserSettlementScheduleDto> result = service.getUserSchedule(userId);
                return ResponseEntity.ok(result);
        }

        @PutMapping()
        public ResponseEntity<ApiResponse<Boolean>> updateUserSchedule(
                        HttpServletRequest request,
                        @RequestBody Set<String> weekdays) {

                Integer userId = (Integer) request.getAttribute("currentUserId");

                return ResponseEntity.ok(service.updateUserSchedule(userId, weekdays));
        }

}