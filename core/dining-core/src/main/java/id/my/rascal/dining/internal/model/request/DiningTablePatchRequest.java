package id.my.rascal.dining.internal.model.request;

import java.util.Optional;

public record DiningTablePatchRequest(
    Optional<String> tableNumber
) {
    public DiningTablePatchRequest(String tableNumber) {
        this(Optional.ofNullable(tableNumber));
    }

    public boolean isEmptyPatch() {
        return tableNumber.isEmpty();
    }
}
