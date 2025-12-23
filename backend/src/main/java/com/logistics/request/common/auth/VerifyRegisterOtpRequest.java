package com.logistics.request.common.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRegisterOtpRequest {
    private String email; 
    private String otp;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
