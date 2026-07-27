package id.my.rascal.auth.internal.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorityRequest(
    @NotBlank(message = "Authority name can't be blank")
    @Size(min = 3, max = 30, message = "Valid authority name are between 3 to 30 characters")
    String name
) {}
