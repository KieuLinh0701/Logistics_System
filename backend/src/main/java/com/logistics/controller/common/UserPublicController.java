package com.logistics.controller.common;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
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

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/user")
public class UserPublicController {

    @Autowired
    private UserPublicService userService;

    @PostMapping("/password/update")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@RequestBody UpdatePasswordRequest request) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();

        if (request.getNewPassword() == null || request.getOldPassword() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        userService.updatePassword(accountId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/email/update")
    public ResponseEntity<?> sendEmailUpdateOTP(@RequestBody UpdateEmailRequest request) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();

        if (request.getNewEmail() == null || request.getPassword() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        userService.sendEmailUpdateOTP(accountId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/email/verify-otp")
    public ResponseEntity<?> verifyEmailUpdateOTP(@RequestBody VerifyEmailUpdateOTPRequest verifyEmailUpdateOTPRequest,
                                                  HttpServletRequest request) {
        Integer accountId = SecurityUtils.getAuthenticatedAccountId();
        Integer roleId = (Integer) request.getAttribute("currentRoleId");

        if (verifyEmailUpdateOTPRequest.getOtp() == null || verifyEmailUpdateOTPRequest.getNewEmail() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        return ResponseEntity.ok(ApiResponse.success(userService.verifyEmailUpdateOTP(accountId, verifyEmailUpdateOTPRequest, roleId)));
    }

    @PutMapping(value = "/profile/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(@ModelAttribute UpdateProfileRequest request) {
        Integer userId = SecurityUtils.getAuthenticatedUserId();

        if (request.getFirstName() == null || request.getLastName() == null || request.getPhoneNumber() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request)));
    }
}