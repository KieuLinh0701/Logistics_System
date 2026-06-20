package com.logistics.request.common.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VerifyResetOtpRequest {
    private String email;
    private String otp;

    @Override
    public String toString() {
        return "VerifyResetOtpRequest{email='" + email + "', otp='******'}";
    }
}