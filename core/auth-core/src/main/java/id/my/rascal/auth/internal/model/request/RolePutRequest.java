package id.my.rascal.auth.internal.model.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record RolePutRequest(
    @NotBlank(message = "Role name can't be blank")
    @Size(min = 3, max = 20, message = "Valid role name are between 3 to 20 characters")
    String name,

    @NotEmpty(message = "Authority IDs can't be empty")
    Set<Long> authorityIds
) {}
