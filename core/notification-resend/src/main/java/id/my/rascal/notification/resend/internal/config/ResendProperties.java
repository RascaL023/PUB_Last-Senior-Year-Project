package id.my.rascal.notification.resend.internal.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "resend")
public record ResendProperties(
    @NotBlank String baseUrl,
    @NotBlank String apiKey,
    @NotBlank String fromEmail,
    String senderName
) {}
