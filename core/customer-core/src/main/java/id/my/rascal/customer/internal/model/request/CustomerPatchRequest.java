package id.my.rascal.customer.internal.model.request;

import java.util.Optional;

import jakarta.validation.constraints.Size;

public record CustomerPatchRequest(
    @Size(min = 3, max = 50, message = "Name must be 3-50 characters")
    String name,

    @Size(max = 20, message = "Phone must be at most 20 characters")
    String phone,

    @Size(max = 500, message = "Notes must be at most 500 characters")
    String notes
) {
    public boolean isEmptyPatch() {
        return name == null 
            && phone == null 
            && notes == null;
    }

    public Optional<String> nameOpt() {
        return Optional.ofNullable(name);
    }

    public Optional<String> phoneOpt() {
        return Optional.ofNullable(phone);
    }

    public Optional<String> notesOpt() {
        return Optional.ofNullable(notes);
    }

}
