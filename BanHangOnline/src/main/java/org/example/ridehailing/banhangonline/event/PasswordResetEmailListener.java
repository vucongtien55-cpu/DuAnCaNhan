package org.example.ridehailing.banhangonline.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnBean(JavaMailSender.class)
public class PasswordResetEmailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetEmailListener.class);

    private final JavaMailSender mailSender;
    private final String frontendBaseUrl;
    private final String senderAddress;

    public PasswordResetEmailListener(
            JavaMailSender mailSender,
            @Value("${app.frontend-base-url:http://localhost:8080}") String frontendBaseUrl,
            @Value("${app.mail.from:}") String senderAddress) {
        this.mailSender = mailSender;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.senderAddress = senderAddress;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendResetLink(PasswordResetRequested event) {
        String resetUrl = frontendBaseUrl + "/#/reset-password?token="
                + URLEncoder.encode(event.rawToken(), StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(event.email());
        message.setSubject("Đặt lại mật khẩu Marketplace");
        message.setText("Bạn vừa yêu cầu đặt lại mật khẩu Marketplace.\n\n"
                + "Mở liên kết sau trong vòng 30 phút; liên kết chỉ dùng được một lần:\n"
                + resetUrl + "\n\nNếu bạn không yêu cầu, hãy bỏ qua email này.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            LOGGER.warn("Không thể gửi email đặt lại mật khẩu; thông tin người nhận được giữ kín.");
        }
    }
}
