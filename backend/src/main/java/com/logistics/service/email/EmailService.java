package com.logistics.service.email;

import com.logistics.config.properties.MailProperties;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final MailProperties mailProperties;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    private static final String PRIMARY = "#1c3d90";
    private static final String PRIMARY_LIGHT = "#a8bde8";
    private static final String BG = "#f4f6fb";
    private static final String BORDER = "#d0d8ef";
    private static final String TEXT_MAIN = "#333333";
    private static final String TEXT_MUTED = "#888888";
    private static final String TEXT_ACCENT = "#1c3d90";

    private final String LOGIN_URL = allowedOrigins + "/login";

    private String wrapLayout(String subtitle, String title, String bodyContent) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:0;background:#eef1f8;font-family:Arial,sans-serif'>"
                + "<div style='max-width:520px;margin:32px auto;background:#fff;border-radius:10px;overflow:hidden;border:0.5px solid " + BORDER + "'>"
                + "<div style='background:" + PRIMARY + ";padding:24px 28px'>"
                + "<p style='margin:0;color:" + PRIMARY_LIGHT + ";font-size:12px'>" + subtitle + "</p>"
                + "<p style='margin:4px 0 0;color:#fff;font-size:20px;font-weight:600'>" + title + "</p>"
                + "</div>"
                + "<div style='padding:24px 28px'>" + bodyContent + "</div>"
                + "<div style='border-top:0.5px solid " + BORDER + ";padding:14px 28px'>"
                + "<p style='margin:0;font-size:12px;color:" + TEXT_MUTED + ";line-height:1.6'>Trân trọng,<br>UTE Logistics</p>"
                + "</div>"
                + "</div></body></html>";
    }

    private String p(String text) {
        return "<p style='margin:0 0 10px;font-size:14px;color:" + TEXT_MAIN + ";line-height:1.7'>" + text + "</p>";
    }

    private String infoBox(String... rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:" + BG + ";border-left:3px solid " + PRIMARY + ";border-radius:0 6px 6px 0;padding:14px 16px;margin:16px 0'>");
        for (String row : rows) {
            String[] parts = row.split("\\|", 2);
            sb.append("<div style='display:flex;justify-content:space-between;font-size:13px;padding:5px 0;border-bottom:0.5px solid " + BORDER + "'>"
                    + "<span style='color:" + TEXT_MUTED + "'>" + parts[0] + "</span>"
                    + "<span style='color:" + TEXT_ACCENT + ";font-weight:500'>" + parts[1] + "</span>"
                    + "</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private void send(String to, String subject, String htmlBody) {
        Email from = new Email(mailProperties.getUsername());
        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlBody);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.info("SendGrid response [{}] for email to: {}", response.getStatusCode(), to);
        } catch (IOException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendOTPEmail(String to, String otp, String subject) {
        String body = p("Xin chào,")
                + p("Mã OTP của bạn là:")
                + "<div style='background:" + BG + ";border:0.5px solid " + BORDER + ";border-radius:8px;padding:18px;text-align:center;margin:16px 0'>"
                + "<p style='margin:0 0 6px;font-size:11px;color:" + TEXT_MUTED + ";text-transform:uppercase;letter-spacing:1px'>Mã xác thực</p>"
                + "<p style='margin:0;font-size:30px;font-weight:700;letter-spacing:10px;color:" + PRIMARY + ";font-family:monospace'>" + otp + "</p>"
                + "<p style='margin:8px 0 0;font-size:12px;color:#b07a00'>&#x23F1; Có hiệu lực trong 5 phút</p>"
                + "</div>"
                + p("Vui lòng không chia sẻ mã này cho bất kỳ ai.");
        send(to, subject, wrapLayout("UTE Logistics", "Xác thực OTP", body));
    }

    @Async
    public void sendAlertEmail(String to, String subject, String alertMessage) {
        String body = p("Xin chào,")
                + "<div style='background:#fff8e1;border-left:3px solid #e6a817;border-radius:0 6px 6px 0;padding:12px 16px;margin:14px 0;font-size:13px;color:#7a5500'>"
                + "&#9888; " + alertMessage
                + "</div>"
                + p("Nếu bạn không thực hiện hành động này, vui lòng liên hệ bộ phận hỗ trợ ngay.");
        send(to, subject, wrapLayout("UTE Logistics", "Thông báo bảo mật", body));
    }

    @Async
    public void sendNewEmployeeAccountEmail(String to, String tempPassword, String firstName, String lastName) {
        String displayName = lastName + " " + firstName;
        String body = p("Xin chào <strong>" + displayName + "</strong>,")
                + p("Tài khoản nhân viên của bạn đã được tạo thành công. Thông tin đăng nhập tạm thời:")
                + infoBox("Email|" + to, "Mật khẩu tạm thời|" + tempPassword)
                + p("Vui lòng đăng nhập và thay đổi mật khẩu ngay sau lần đăng nhập đầu tiên.");
        send(to, "Thông báo tài khoản nhân viên mới", wrapLayout("UTE Logistics", "Tài khoản nhân viên mới", body));
    }


    @Async
    public void sendRecruitmentAccountEmail(String to, String tempPassword, String firstName, String lastName,
                                            String jobTitle, String officeName, String shift, String startDate) {
        String f = firstName == null ? "" : firstName.trim();
        String l = lastName == null ? "" : lastName.trim();
        String displayName;
        if (!f.isEmpty() && !l.isEmpty()) {
            displayName = f.equalsIgnoreCase(l) ? f : l + " " + f;
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
                case "FULL_DAY":   shiftDisplay = "Cả ngày";  break;
                case "MORNING":    shiftDisplay = "Ca sáng";  break;
                case "AFTERNOON":
                case "EVENING":    shiftDisplay = "Ca chiều"; break;
                case "NIGHT":      shiftDisplay = "Ca đêm";   break;
                default:
                    shiftDisplay = s.replace('_', ' ').toLowerCase();
                    if (!shiftDisplay.isEmpty()) {
                        shiftDisplay = Character.toUpperCase(shiftDisplay.charAt(0)) + shiftDisplay.substring(1);
                    }
            }
        }

        String loginUrl = allowedOrigins + "/login";
        String body = p("Xin chào <strong>" + displayName + "</strong>,")
                + p("Chúng tôi rất vui mừng thông báo bạn đã trúng tuyển vào vị trí sau:")
                + infoBox(
                "Vị trí|" + (jobTitle != null ? jobTitle : "Đang cập nhật"),
                "Bưu cục làm việc|" + (officeName != null ? officeName : "Đang cập nhật"),
                "Ca làm việc|" + shiftDisplay,
                "Ngày bắt đầu|" + (startDate != null ? startDate : "Đang cập nhật")
        )
                + p("Thông tin tài khoản làm việc của bạn:")
                + infoBox("Email đăng nhập|" + to, "Mật khẩu tạm thời|" + tempPassword)
                + p("Vui lòng đăng nhập và đổi mật khẩu sau lần đăng nhập đầu tiên.")
                + "<a href='" + loginUrl + "' style='display:inline-block;margin-top:8px;background:" + PRIMARY + ";color:#fff;"
                + "text-decoration:none;padding:10px 24px;border-radius:6px;font-size:13px;font-weight:500'>Đăng nhập ngay</a>";
        send(to, "Kết quả tuyển dụng và thông tin tài khoản làm việc tại UTE Logistics",
                wrapLayout("UTE Logistics", "Chúc mừng! Bạn đã trúng tuyển", body));
    }

    @Async
    public void sendRecruitmentRejectionEmail(String to) {
        String body = p("Xin chào,")
                + p("UTE Logistics chân thành cảm ơn bạn đã quan tâm và ứng tuyển vào vị trí tuyển dụng tại công ty.")
                + "<div style='background:" + BG + ";border-radius:8px;padding:14px 16px;margin:14px 0;font-size:13px;color:#555;line-height:1.7;border:0.5px solid " + BORDER + "'>"
                + "Sau quá trình xem xét, chúng tôi rất tiếc phải thông báo rằng hồ sơ của bạn hiện chưa phù hợp với yêu cầu tuyển dụng ở thời điểm hiện tại.<br><br>"
                + "Quyết định này không phản ánh năng lực cá nhân của bạn. Chúng tôi sẽ lưu thông tin của bạn và liên hệ khi có cơ hội phù hợp hơn trong tương lai."
                + "</div>";
        send(to, "Thông báo kết quả ứng tuyển tại UTE Logistics",
                wrapLayout("UTE Logistics", "Kết quả ứng tuyển", body));
    }
}