package id.my.rascal.auth.internal.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotEmpty(message = "Email must be filled")
    @Email(message = "Invalid email format")
    String email,

    @NotEmpty(message = "Password must be filled")
    @Size(min = 8, message = "Username/password wrong")
    String password
) {
}
