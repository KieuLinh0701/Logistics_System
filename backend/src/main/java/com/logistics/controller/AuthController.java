package com.logistics.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.logistics.dto.auth.ForgotPasswordRequest;
import com.logistics.dto.auth.LoginRequest;
import com.logistics.dto.auth.RegisterRequest;
import com.logistics.dto.auth.ResetPasswordRequest;
import com.logistics.dto.auth.VerifyRegisterOtpRequest;
import com.logistics.dto.auth.VerifyResetOtpRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getEmail() == null || request.getPassword() == null ||
                request.getFirstName() == null || request.getLastName() == null || request.getPhoneNumber() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng điền đầy đủ thông tin", null));
        }

        String password = request.getPassword();

        if (password.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Mật khẩu phải có ít nhất 6 ký tự", null));
        }

        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.])[A-Za-z\\d@$!%*?&.]{6,}$";
        if (!password.matches(passwordPattern)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false,
                            "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt", null));
        }

        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyAndRegisterUser(@RequestBody VerifyRegisterOtpRequest request) {
        if (request.getEmail() == null || request.getOtp() == null ||
                request.getPassword() == null || request.getFirstName() == null ||
                request.getLastName() == null || request.getPhoneNumber() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng điền đầy đủ thông tin", null));
        }

        return ResponseEntity.ok(authService.verifyAndRegisterUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getIdentifier() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng nhập email hoặc số điện thoại và mật khẩu", null));
        }

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getIdentifier() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng nhập email hoặc số điện thoại", null));
        }
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/password/verify-otp")
    public ResponseEntity<?> verifyResetOtp(@RequestBody VerifyResetOtpRequest request) {
        if (request.getOtp() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Vui lòng nhập mã OTP", null));
        }

        return ResponseEntity.ok(authService.verifyResetOtp(request));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        System.out.println("Debug message");
        if (request.getNewPassword() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Vui lòng nhập mật khẩu", null));
        }

        return ResponseEntity.ok(authService.resetPassword(request));
    }

    // // 🟢 Get profile
    // @GetMapping("/profile")
    // public ResponseEntity<?> getProfile(@RequestAttribute("userId") Long userId)
    // {
    // return ResponseEntity.ok(authService.getUserProfile(userId));
    // }

    // // 🟢 Update profile
    // @PutMapping("/profile")
    // public ResponseEntity<?> updateProfile(@RequestAttribute("userId") Long
    // userId,
    // @RequestBody UpdateProfileRequest request) {
    // return ResponseEntity.ok(authService.updateUserProfile(userId, request));
    // }

    // // 🟢 Update avatar
    // @PostMapping("/avatar")
    // public ResponseEntity<?> updateAvatar(@RequestAttribute("userId") Long
    // userId,
    // @RequestParam("file") MultipartFile file) {
    // if (file.isEmpty()) {
    // return ResponseEntity.badRequest().body(new ApiResponse(false, "Không có file
    // upload"));
    // }

    // return ResponseEntity.ok(authService.updateUserAvatar(userId, file));
    // }

    // // 🟢 Get assignable roles
    // @GetMapping("/roles")
    // public ResponseEntity<?> getAssignableRoles(@RequestAttribute("userId") Long
    // userId) {
    // return ResponseEntity.ok(authService.getAssignableRoles(userId));
    // }

    // // 🟢 Update password
    // @PutMapping("/update-password")
    // public ResponseEntity<?> updatePassword(@RequestAttribute("userId") Long
    // userId,
    // @RequestBody UpdatePasswordRequest request) {
    // if (request.getCurrentPassword() == null || request.getNewPassword() == null
    // || request.getConfirmPassword() == null) {
    // return ResponseEntity.badRequest().body(new ApiResponse(false, "Vui lòng điền
    // đầy đủ thông tin"));
    // }

    // if (request.getNewPassword().length() < 6) {
    // return ResponseEntity.badRequest().body(new ApiResponse(false, "Mật khẩu phải
    // có ít nhất 6 ký tự"));
    // }

    // if (!request.getNewPassword().equals(request.getConfirmPassword())) {
    // return ResponseEntity.badRequest().body(new ApiResponse(false, "Mật khẩu mới
    // và xác nhận không khớp"));
    // }

    // return ResponseEntity.ok(authService.updatePassword(userId, request));
    // }

    // // 🟢 Send OTP for updating email
    // @PostMapping("/send-email-otp")
    // public ResponseEntity<?> sendEmailOtpUpdateEmail(@RequestBody EmailRequest
    // request) {
    // if (request.getEmail() == null) {
    // return ResponseEntity.badRequest().body(new ApiResponse(false, "Vui lòng nhập
    // email"));
    // }

    // return
    // ResponseEntity.ok(authService.sendEmailOtpUpdateEmail(request.getEmail()));
    // }

    // // 🟢 Verify email OTP
    // @PostMapping("/verify-email-otp")
    // public ResponseEntity<?> verifyEmailOtp(@RequestBody VerifyEmailOtpRequest
    // request) {
    // if (request.getEmail() == null || request.getOtp() == null) {
    // return ResponseEntity.badRequest().body(new ApiResponse(false, "Vui lòng nhập
    // email và mã OTP"));
    // }

    // return ResponseEntity.ok(authService.verifyEmailOtp(request.getEmail(),
    // request.getOtp()));
    // }

    // // 🟢 Update email
    // @PutMapping("/update-email")
    // public ResponseEntity<?> updateEmail(@RequestAttribute("userId") Long userId,
    // @RequestBody EmailRequest request) {
    // if (request.getEmail() == null) {
    // return ResponseEntity.badRequest().body(new ApiResponse(false, "Vui lòng nhập
    // email mới"));
    // }

    // return ResponseEntity.ok(authService.updateEmail(userId,
    // request.getEmail()));
    // }
}