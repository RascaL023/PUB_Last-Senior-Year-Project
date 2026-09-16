package id.my.rascal.notification.internal;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import id.my.rascal.notification.api.EmailSendResultApi;
import id.my.rascal.notification.api.EmailSenderApi;
import id.my.rascal.notification.api.SendEmailRequestApi;

@Service
public class NotificationService {

    private final EmailSenderApi emailSender;
    private final EmailTemplate templateService;

    public NotificationService(EmailSenderApi emailSender, EmailTemplate templateService) {
        this.emailSender = emailSender;
        this.templateService = templateService;
    }

    public EmailSendResultApi sendResetPasswordEmail(String to, String resetLink) {
        String html = templateService.buildResetPasswordHtml(resetLink);
        String text = templateService.buildResetPasswordText(resetLink);
        SendEmailRequestApi request = new SendEmailRequestApi(
            to,
            "Reset Your Password",
            html,
            text,
            Map.of("Idempotency-Key", UUID.randomUUID().toString())
        );
        return emailSender.sendEmail(request);
    }

}
