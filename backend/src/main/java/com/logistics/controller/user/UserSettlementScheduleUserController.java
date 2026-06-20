package com.logistics.controller.user;

import com.logistics.dto.UserSettlementScheduleDto;
import com.logistics.response.ApiResponse;
import com.logistics.service.user.UserSettlementScheduleUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/user/user-settlement-batchs")
@Tag(name = "User - Settlement Schedule", description = "Quản lý lịch trình đối soát tài chính định kỳ của người dùng theo các ngày trong tuần")
public class UserSettlementScheduleUserController {

        @Autowired
        private UserSettlementScheduleUserService service;

        @GetMapping
        public ResponseEntity<ApiResponse<UserSettlementScheduleDto>> getUserSchedule(
                        HttpServletRequest request) {
                Integer userId = (Integer) request.getAttribute("currentUserId");

                UserSettlementScheduleDto result = service.getUserSchedule(userId);
                return ResponseEntity.ok(ApiResponse.success(result));
        }

        @PutMapping()
        public ResponseEntity<ApiResponse<Void>> updateUserSchedule(
                        HttpServletRequest request,
                        @RequestBody Set<String> weekdays) {

                Integer userId = (Integer) request.getAttribute("currentUserId");

                service.updateUserSchedule(userId, weekdays);
                return ResponseEntity.ok(ApiResponse.success(null));
        }

}