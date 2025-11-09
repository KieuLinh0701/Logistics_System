package com.logistics.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOTPEmail(String to, String otp, String subject) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);

        String content = "Xin chào,\n"
                + "Mã OTP của bạn là: " + otp + "\n"
                + "Có hiệu lực trong 5 phút.\n"
                + "Trân trọng,\nĐội ngũ hỗ trợ.";

        message.setText(content);
        mailSender.send(message);
    }

    public void sendAlertEmail(String to, String subject, String alertMessage) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);

        String content = "Xin chào,\n"
                + alertMessage + "\n"
                + "Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.\n"
                + "Trân trọng,\nĐội ngũ hỗ trợ.";

        message.setText(content);
        mailSender.send(message);
    }
}