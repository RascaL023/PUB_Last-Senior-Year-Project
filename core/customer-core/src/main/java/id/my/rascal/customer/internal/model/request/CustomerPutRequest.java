package id.my.rascal.customer.internal.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerPutRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50, message = "Name must be 3-50 characters")
    String name,

    @Size(max = 20, message = "Phone must be at most 20 characters")
    @Pattern(
        regexp = "^(\\+62|62|0)8[1-9][0-9]{6,10}$", 
        message = "Invalid phone number (e.g: 08123456789)"
    )
    String phone,

    @Size(max = 500, message = "Notes must be at most 500 characters")
    String notes
) {}
