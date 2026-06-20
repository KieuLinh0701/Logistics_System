package com.logistics.controller.common;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.AccountErrorCode;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.common.auth.*;
import com.logistics.response.ApiResponse;
import com.logistics.response.AuthResponse;
import com.logistics.service.common.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Xác thực người dùng: Đăng nhập, đăng ký, khôi phục mật khẩu")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @Audit(
            entity = EntityType.ACCOUNT,
            action = AuditLogAction.REGISTER,
            description = AuditLogDescriptionConstant.AUTH_REGISTER
    )
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getEmail() == null || request.getPassword() == null ||
                request.getFirstName() == null || request.getLastName() == null || request.getPhoneNumber() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        String password = request.getPassword();

        if (password.length() < 6) {
            throw new AppException(AccountErrorCode.ACCOUNT_PASSWORD_TOO_SHORT);
        }

        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.])[A-Za-z\\d@$!%*?&.]{6,}$";
        if (!password.matches(passwordPattern)) {
            throw new AppException(AccountErrorCode.ACCOUNT_PASSWORD_POLICY_VIOLATION);
        }

        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyAndRegisterUser(@RequestBody VerifyRegisterOtpRequest request) {
        if (request.getEmail() == null || request.getOtp() == null ||
                request.getPassword() == null || request.getFirstName() == null ||
                request.getLastName() == null || request.getPhoneNumber() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        return ResponseEntity.ok(ApiResponse.success(authService.verifyAndRegisterUser(request)));
    }

    @PostMapping("/login")
    @Audit(
            entity = EntityType.ACCOUNT,
            action = AuditLogAction.LOGIN,
            description = AuditLogDescriptionConstant.AUTH_LOGIN
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/choose-role")
    public ResponseEntity<?> chooseRole(@RequestBody ChooseRoleRequest request) {
            AuthResponse authResponse = authService.chooseRole(request);
            return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<?> forgotPasswordEmail(@RequestBody ForgotPasswordEmailRequest request) {
        if (request.getEmail() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        authService.forgotPasswordEmail(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/password/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyResetOtp(@RequestBody VerifyResetOtpRequest request) {
        if (request.getOtp() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        authService.verifyResetOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/password/reset")
    @Audit(
            entity = EntityType.ACCOUNT,
            action = AuditLogAction.PASSWORD_RESET,
            description = AuditLogDescriptionConstant.AUTH_PASSWORD_RESET
    )
    public ResponseEntity<ApiResponse<Void>> forgotPasswordReset(@RequestBody ForgotPasswordResetRequest request) {
        if (request.getNewPassword() == null) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELD);
        }

        authService.forgotPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}