package com.logistics.service.common;

import com.logistics.request.common.user.UpdateEmailRequest;
import com.logistics.request.common.user.UpdatePasswordRequest;
import com.logistics.request.common.user.UpdateProfileRequest;
import com.logistics.request.common.user.VerifyEmailUpdateOTPRequest;
import com.logistics.response.AuthResponse;
import lombok.NonNull;

public interface UserPublicService {
    void updatePassword(@NonNull Integer accountId, UpdatePasswordRequest request);
    void sendEmailUpdateOTP(@NonNull Integer accountId, UpdateEmailRequest request);
    AuthResponse verifyEmailUpdateOTP(@NonNull Integer accountId, VerifyEmailUpdateOTPRequest request, Integer roleId);
    String updateProfile(@NonNull Integer userId, @NonNull UpdateProfileRequest updatedUser);
}