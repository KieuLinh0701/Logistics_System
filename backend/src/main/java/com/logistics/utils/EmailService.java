package com.logistics.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final String LOGIN_URL = allowedOrigins + "/login";

    @Async
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

    @Async
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

    @Async
    public void sendNewEmployeeAccountEmail(String to, String tempPassword, String firstName, String lastName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Thông báo tài khoản nhân viên mới");

        String content = "Xin chào " + lastName + " " + firstName + ",\n\n"
                + "Tài khoản nhân viên của bạn đã được tạo thành công.\n"
                + "Thông tin đăng nhập tạm thời như sau:\n"
                + "Email: " + to + "\n"
                + "Mật khẩu tạm thời: " + tempPassword + "\n\n"
                + "Vui lòng đăng nhập và thay đổi mật khẩu ngay sau lần đăng nhập đầu tiên.\n\n"
                + "Trân trọng,\nĐội ngũ quản lý.";

        message.setText(content);
        mailSender.send(message);
    }

    @Async
    public void sendRecruitmentAccountEmail(String to, String tempPassword, String firstName, String lastName, String jobTitle, String officeName, String shift, String startDate) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Kết quả tuyển dụng và thông tin tài khoản làm việc tại UTE Logistics");
        String f = firstName == null ? "" : firstName.trim();
        String l = lastName == null ? "" : lastName.trim();
        String displayName;
        if (!f.isEmpty() && !l.isEmpty()) {
            if (f.equalsIgnoreCase(l)) {
                displayName = f;
            } else {
                displayName = l + " " + f;
            }
        } else if (!l.isEmpty()) {
            displayName = l;
        } else {
            displayName = f;
        }

        String shiftDisplay;
        if (shift == null) {
            shiftDisplay = "Đang cập nhật";
        } else {
            String s = shift.trim().toUpperCase();
            switch (s) {
                case "FULL_DAY":
                    shiftDisplay = "Cả ngày";
                    break;
                case "MORNING":
                    shiftDisplay = "Ca sáng";
                    break;
                case "AFTERNOON":
                case "EVENING":
                    shiftDisplay = "Ca chiều";
                    break;
                case "NIGHT":
                    shiftDisplay = "Ca đêm";
                    break;
                default:
                    shiftDisplay = s.replace('_', ' ').toLowerCase();
                    if (shiftDisplay.length() > 0) {
                        shiftDisplay = shiftDisplay.substring(0, 1).toUpperCase() + shiftDisplay.substring(1);
                    }
                    break;
            }
        }

        String content = "Xin chào " + (displayName == null ? "" : displayName) + ",\n\n"
                + "Chúng tôi rất vui mừng thông báo bạn đã trúng tuyển vào vị trí sau:\n\n"
                + "Vị trí: " + (jobTitle != null ? jobTitle : "Đang cập nhật") + "\n"
                + "Bưu cục làm việc: " + (officeName != null ? officeName : "Đang cập nhật") + "\n"
                + "Ca làm việc: " + shiftDisplay + "\n"
                + "Ngày bắt đầu: " + (startDate != null ? startDate : "Đang cập nhật") + "\n\n"
                + "Thông tin tài khoản làm việc của bạn:\n\n"
                + "Email đăng nhập: " + to + "\n"
                + "Mật khẩu tạm thời: " + tempPassword + "\n\n"
                + "Vui lòng đăng nhập và đổi mật khẩu sau lần đăng nhập đầu tiên.\n\n"
                + "Link đăng nhập:\n"
                + LOGIN_URL + "\n\n"
                + "Trân trọng,\nUTE Logistics";

        message.setText(content);
        mailSender.send(message);
    }

    @Async
    public void sendRecruitmentRejectionEmail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Thông báo kết quả ứng tuyển tại UTE Logistics");

        String content = "Xin chào,\n\n"
                + "UTE Logistics chân thành cảm ơn bạn đã quan tâm và ứng tuyển vào vị trí tuyển dụng tại công ty.\n\n"
                + "Sau quá trình xem xét, chúng tôi rất tiếc phải thông báo rằng hồ sơ của bạn hiện chưa phù hợp với yêu cầu tuyển dụng ở thời điểm hiện tại.\n\n"
                + "Quyết định này không phản ánh năng lực cá nhân của bạn.\n\n"
                + "Chúng tôi sẽ lưu thông tin của bạn và liên hệ khi có cơ hội phù hợp hơn trong tương lai.\n\n"
                + "Trân trọng,\nUTE Logistics";

        message.setText(content);
        mailSender.send(message);
    }
}