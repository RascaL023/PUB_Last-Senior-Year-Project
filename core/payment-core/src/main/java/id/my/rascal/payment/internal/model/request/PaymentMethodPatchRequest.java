package id.my.rascal.payment.internal.model.request;

import java.util.Optional;

public record PaymentMethodPatchRequest(
    Optional<String> code,
    Optional<String> name,
    Optional<Boolean> isActive
) {
    public PaymentMethodPatchRequest(String code, String name, Boolean isActive) {
        this(
            Optional.ofNullable(code),
            Optional.ofNullable(name),
            Optional.ofNullable(isActive)
        );
    }

    public boolean isEmptyPatch() {
        return code.isEmpty() && name.isEmpty() && isActive.isEmpty();
    }
}
