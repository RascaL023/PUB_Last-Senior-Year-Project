package id.my.rascal.auth.internal.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.entity.PasswordResetToken;
import id.my.rascal.auth.internal.entity.UserAuth;
import id.my.rascal.auth.internal.repository.PasswordResetTokenRepository;
import id.my.rascal.auth.internal.repository.UserAuthRepository;
import id.my.rascal.auth.internal.model.request.ForgotPasswordRequest;
import id.my.rascal.auth.internal.model.request.ResetPasswordRequest;
import id.my.rascal.auth.internal.model.response.ForgotPasswordResponse;
import id.my.rascal.auth.internal.model.response.ResetPasswordResponse;
import id.my.rascal.notification.api.SendEmailRequestApi;
import id.my.rascal.notification.api.EmailSenderApi;
import id.my.rascal.notification.api.NotificationException;
import id.my.rascal.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private final UserAuthRepository userAuthRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserAuthService userAuthService;
    private final EmailSenderApi emailSender;
    private final PasswordEncoder passwordEncoder;
    private final String redirectBaseUrl;
    private final int tokenExpiryMinutes;

    public PasswordResetService(
        UserAuthRepository userAuthRepository,
        PasswordResetTokenRepository tokenRepository,
        UserAuthService userAuthService,
        EmailSenderApi emailSender,
        PasswordEncoder passwordEncoder,
        @Value("${app.dev-base-url:https://dev.rascal.my.id}") String redirectBaseUrl,
        @Value("${notification.reset-token-expiry-minutes:60}") int tokenExpiryMinutes
    ) {
        this.userAuthRepository = userAuthRepository;
        this.tokenRepository = tokenRepository;
        this.userAuthService = userAuthService;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.redirectBaseUrl = redirectBaseUrl;
        this.tokenExpiryMinutes = tokenExpiryMinutes;
    }

    @Transactional
    public ForgotPasswordResponse requestReset(ForgotPasswordRequest request) {
        UserAuth user = userAuthRepository.findActiveByEmail(request.email()).orElse(null);
        if (user != null) {
            String rawToken = UUID.randomUUID().toString();
            String tokenHash = passwordEncoder.encode(rawToken);

            tokenRepository.deleteByUserAuthId(user.getId());

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserAuth(user);
            resetToken.setTokenHash(tokenHash);
            resetToken.setExpiresAt(Instant.now().plusSeconds(tokenExpiryMinutes * 60L));
            resetToken.setCreatedAt(Instant.now());
            tokenRepository.save(resetToken);

            String resetLink = redirectBaseUrl + "/reset-password?token=" + user.getId() + "_" + rawToken;
            
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Reset Your Password</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.1); overflow: hidden; }
                        .header { background-color: #0046c0; color: #ffffff; padding: 20px; text-align: center; }
                        .content { padding: 30px; color: #333333; line-height: 1.6; }
                        .button-container { text-align: center; margin: 30px 0; }
                        .button { background-color: #0046c0; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block; }
                        .footer { background-color: #f4f7f6; color: #888888; padding: 15px; text-align: center; font-size: 12px; border-top: 1px solid #eaaaaaa; }
                        a { color: #0046c0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>Password Reset Request</h2>
                        </div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>We received a request to reset your password. If you didn't make this request, you can safely ignore this email.</p>
                            <p>To reset your password, please click the button below:</p>
                            <div class="button-container">
                                <a href="%s" class="button">Reset Password</a>
                            </div>
                            <p>This link will expire in <strong>%d minutes</strong>.</p>
                            <p>Or copy and paste this URL into your browser:</p>
                            <p><a href="%s">%s</a></p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Rascal. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resetLink, tokenExpiryMinutes, resetLink, resetLink);
                
            String textContent = """
                Hello,
                
                We received a request to reset your password. If you didn't make this request, you can safely ignore this email.
                
                To reset your password, please copy and paste the following URL into your browser:
                %s
                
                This link will expire in %d minutes.
                
                © 2026 Rascal. All rights reserved.
                """.formatted(resetLink, tokenExpiryMinutes);
                
            CompletableFuture.runAsync(() -> {
                try {
                    emailSender.sendEmail(new SendEmailRequestApi(
                        request.email(),
                        "Reset Your Password",
                        htmlContent,
                        textContent,
                        Map.of("Idempotency-Key", UUID.randomUUID().toString())
                    ));
                } catch (NotificationException e) {
                    log.error("Failed to send reset email to {}: {}", request.email(), e.getMessage());
                }
            });
        }

        return new ForgotPasswordResponse("If the email is registered, a reset link has been sent");
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        String reqToken = request.token();
        if (reqToken == null || !reqToken.contains("_"))
            throw new BadRequestException("Invalid or expired reset token format");
        
        String[] parts = reqToken.split("_", 2);
        Long userId;
        try { userId = Long.parseLong(parts[0]); } 
        catch (NumberFormatException e) { throw new BadRequestException("Invalid or expired reset token format"); }
        String rawToken = parts[1];

        PasswordResetToken validToken = tokenRepository.findFirstByUserAuthId(userId)
            .filter(t -> t.getExpiresAt().isAfter(Instant.now()) && t.getRevokedAt() == null)
            .filter(t -> passwordEncoder.matches(rawToken, t.getTokenHash()))
            .orElse(null);

        if (validToken == null)
            throw new BadRequestException("Invalid or expired reset token");

        UserAuth user = validToken.getUserAuth();
        userAuthService.updatePassword(user.getId(), request.newPassword());
        tokenRepository.deleteByUserAuthId(user.getId());

        return new ResetPasswordResponse("Password has been reset");
    }

}
