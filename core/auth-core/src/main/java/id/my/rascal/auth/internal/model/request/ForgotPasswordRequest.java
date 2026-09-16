package id.my.rascal.auth.internal.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record ForgotPasswordRequest(
    @NotEmpty(message = "Email must be filled")
    @Email(message = "Invalid email format")
    String email
) {}
