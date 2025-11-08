package com.logistics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {
    private String email;
    private String otp;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
