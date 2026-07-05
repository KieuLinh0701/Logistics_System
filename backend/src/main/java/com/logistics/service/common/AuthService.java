package com.logistics.service.common;

import com.logistics.request.common.auth.*;
import com.logistics.response.AuthResponse;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse verifyAndRegisterUser(VerifyRegisterOtpRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse chooseRole(ChooseRoleRequest request);

    void forgotPasswordEmail(ForgotPasswordEmailRequest request);

    void verifyResetOtp(VerifyResetOtpRequest request);

    void forgotPasswordReset(ForgotPasswordResetRequest request);
}