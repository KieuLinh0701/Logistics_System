package com.logistics.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.request.common.user.UpdateEmailRequest;
import com.logistics.request.common.user.UpdatePasswordRequest;
import com.logistics.request.common.user.UpdateProfileRequest;
import com.logistics.request.common.user.VerifyEmailUpdateOTPRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.UserPublicService;
import com.logistics.utils.SecurityUtils;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/user")
public class UserPublicController {

    @Autowired
    private UserPublicService userService;

    @PostMapping("/password/update")
    public ResponseEntity<ApiResponse<String>> updatePassword(@RequestBody UpdatePasswordRequest request) {
        Integer accountId;
        try {
            accountId = SecurityUtils.getAuthenticatedAccountId();
        } catch (RuntimeException e) {
            ApiResponse<String> response = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(401).body(response);
        }

        if (request.getNewPassword() == null || request.getOldPassword() == null) {
            ApiResponse<String> response = new ApiResponse<>(false, "Vui lòng nhập đầy đủ thông tin", null);
            return ResponseEntity.badRequest().body(response);
        }

        ApiResponse<String> response = userService.updatePassword(accountId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/update")
    public ResponseEntity<?> sendEmailUpdateOTP(@RequestBody UpdateEmailRequest request) {
        Integer accountId;
        try {
            accountId = SecurityUtils.getAuthenticatedAccountId();
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new ApiResponse<>(false, e.getMessage(), null));
        }

        if (request.getNewEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng nhập đầy đủ thông tin", null));
        }

        return ResponseEntity.ok(userService.sendEmailUpdateOTP(accountId, request));
    }

    @PostMapping("/email/verify-otp")
    public ResponseEntity<?> verifyEmailUpdateOTP(@RequestBody VerifyEmailUpdateOTPRequest request) {
        Integer accountId;
        try {
            accountId = SecurityUtils.getAuthenticatedAccountId();
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new ApiResponse<>(false, e.getMessage(), null));
        }

        if (request.getOtp() == null || request.getNewEmail() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng nhập đầy đủ thông tin", null));
        }

        return ResponseEntity.ok(userService.verifyEmailUpdateOTP(accountId, request));
    }

    @PutMapping(value = "/profile/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(@ModelAttribute UpdateProfileRequest request) {
        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new ApiResponse<>(false, e.getMessage(), null));
        }

        System.out.println("a");

        if (request.getFirstName() == null || request.getLastName() == null || request.getPhoneNumber() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng nhập đầy đủ thông tin", null));
        }

        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }
}