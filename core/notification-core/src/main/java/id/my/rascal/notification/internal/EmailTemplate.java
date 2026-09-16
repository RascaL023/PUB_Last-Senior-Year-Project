package id.my.rascal.notification.internal;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplate {

    public String buildResetPasswordHtml(String resetLink) {
        return """
            <html>
            <body>
                <h2>Reset Your Password</h2>
                <p>Click the button below to reset your password:</p>
                <a href="%s">Reset Password</a>
                <p>If you did not request this, please ignore this email.</p>
            </body>
            </html>
            """.formatted(resetLink);
    }

    public String buildResetPasswordText(String resetLink) {
        return """
            Reset Your Password

            Click the link below to reset your password:
            %s

            If you did not request this, please ignore this email.
            """.formatted(resetLink);
    }

}
