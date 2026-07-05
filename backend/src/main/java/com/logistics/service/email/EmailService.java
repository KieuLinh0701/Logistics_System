package com.logistics.service.email;

public interface EmailService {
    void sendOTPEmail(String to, String otp, String subject);
    void sendAlertEmail(String to, String subject, String alertMessage);
    void sendNewEmployeeAccountEmail(String to, String tempPassword, String firstName, String lastName);
    void sendRecruitmentAccountEmail(String to, String tempPassword, String firstName, String lastName,
                                    String jobTitle, String officeName, String shift, String startDate);
    void sendRecruitmentRejectionEmail(String to);
}
