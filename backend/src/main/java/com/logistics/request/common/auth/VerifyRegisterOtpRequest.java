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

    @Override
    public String toString() {
        return "VerifyRegisterOtpRequest{email='" + email + "', otp='" + otp + "', password='******', firstName='" + firstName + "', lastName='" + lastName + "', phoneNumber='" + phoneNumber + "'}";
    }
}
