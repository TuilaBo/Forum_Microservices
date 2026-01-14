package com.khoavdse170395.authservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:Java Forum <no-reply@example.com>}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String subject, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildOtpHtml(otp, purpose), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    private String buildOtpHtml(String otp, String purpose) {
        return "<html>\n" +
                "<head>\n" +
                "  <style>\n" +
                "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background-color: #f5f5f7; margin: 0; padding: 0; }\n" +
                "    .container { max-width: 520px; margin: 32px auto; background: #ffffff; border-radius: 12px;\n" +
                "                 box-shadow: 0 10px 30px rgba(0,0,0,0.06); overflow: hidden; }\n" +
                "    .header { background: linear-gradient(120deg, #111827, #1e293b); padding: 20px 24px; color: #e5e7eb; }\n" +
                "    .title { margin: 0; font-size: 20px; font-weight: 600; }\n" +
                "    .subtitle { margin: 4px 0 0; font-size: 13px; color: #9ca3af; }\n" +
                "    .content { padding: 24px; color: #111827; font-size: 14px; line-height: 1.6; }\n" +
                "    .label { font-size: 13px; text-transform: uppercase; letter-spacing: 0.08em; color: #6b7280; margin-bottom: 8px; }\n" +
                "    .otp { display: inline-block; letter-spacing: 0.35em; font-size: 24px; font-weight: 700;\n" +
                "           padding: 12px 18px; border-radius: 10px; background: #f3f4f6; border: 1px solid #e5e7eb; }\n" +
                "    .purpose { margin-top: 12px; font-size: 13px; color: #4b5563; }\n" +
                "    .meta { margin-top: 20px; font-size: 12px; color: #6b7280; }\n" +
                "    .footer { padding: 16px 24px 20px; font-size: 11px; color: #9ca3af; background: #f9fafb; }\n" +
                "    .brand { font-weight: 600; color: #111827; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class='container'>\n" +
                "    <div class='header'>\n" +
                "      <p class='title'>Java Forum Security</p>\n" +
                "      <p class='subtitle'>Xác nhận thao tác bảo mật trên tài khoản của bạn</p>\n" +
                "    </div>\n" +
                "    <div class='content'>\n" +
                "      <p>Chào bạn,</p>\n" +
                "      <p>Chúng tôi nhận được một yêu cầu <strong>" + purpose + "</strong> cho tài khoản Java Forum của bạn.</p>\n" +
                "      <p class='label'>Mã xác thực (OTP)</p>\n" +
                "      <div>\n" +
                "        <span class='otp'>" + otp + "</span>\n" +
                "      </div>\n" +
                "      <p class='purpose'>\n" +
                "        Mã này có hiệu lực trong <strong>5 phút</strong>. Tuyệt đối không chia sẻ mã cho bất kỳ ai, kể cả người tự xưng là từ Java Forum.\n" +
                "      </p>\n" +
                "      <p class='meta'>\n" +
                "        Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email hoặc đổi mật khẩu để đảm bảo an toàn cho tài khoản.\n" +
                "      </p>\n" +
                "    </div>\n" +
                "    <div class='footer'>\n" +
                "      <p class='brand'>Java Forum</p>\n" +
                "      <p>Nền tảng thảo luận và chia sẻ kiến thức dành cho cộng đồng Java developers.</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }
}

