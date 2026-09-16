package id.my.rascal.customer.internal.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50, message = "Name must be 3-50 characters")
    String name,

    @Size(max = 255, message = "Email must be at most 255 characters")
    String email,

    @Size(max = 20, message = "Phone must be at most 20 characters")
    String phone,

    @Size(max = 500, message = "Notes must be at most 500 characters")
    String notes
) {}
