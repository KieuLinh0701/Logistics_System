package com.logistics.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.logistics.request.common.auth.ChooseRoleRequest;
import com.logistics.request.common.auth.ForgotPasswordEmailRequest;
import com.logistics.request.common.auth.ForgotPasswordResetRequest;
import com.logistics.request.common.auth.LoginRequest;
import com.logistics.request.common.auth.RegisterRequest;
import com.logistics.request.common.auth.VerifyRegisterOtpRequest;
import com.logistics.request.common.auth.VerifyResetOtpRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.AuthResponse;
import com.logistics.service.common.AuthService;

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
        if (request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng nhập email", null));
        }

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/choose-role")
    public ResponseEntity<?> chooseRole(@RequestBody ChooseRoleRequest request) {
        try {
            AuthResponse authResponse = authService.chooseRole(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Đăng nhập với role thành công", authResponse));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(false, "Lỗi khi chọn role: " + e.getMessage(), null));
        }
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<?> forgotPasswordEmail(@RequestBody ForgotPasswordEmailRequest request) {
        if (request.getEmail() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng nhập email", null));
        }
        return ResponseEntity.ok(authService.forgotPasswordEmail(request));
    }

    @PostMapping("/password/verify-otp")
    public ResponseEntity<?> verifyResetOtp(@RequestBody VerifyResetOtpRequest request) {
        if (request.getOtp() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Vui lòng nhập mã OTP", null));
        }

        return ResponseEntity.ok(authService.verifyResetOtp(request));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> forgotPasswordReset(@RequestBody ForgotPasswordResetRequest request) {
        System.out.println("Debug message");
        if (request.getNewPassword() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Vui lòng nhập mật khẩu", null));
        }

        return ResponseEntity.ok(authService.forgotPasswordReset(request));
    }
}