package id.my.rascal.invoice.internal.model.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record RefundRequest(
    Long paymentId,
    @NotEmpty(message = "orderItemIds must not be empty")
    List<Long> orderItemIds,
    Long requestedBy
) {}
