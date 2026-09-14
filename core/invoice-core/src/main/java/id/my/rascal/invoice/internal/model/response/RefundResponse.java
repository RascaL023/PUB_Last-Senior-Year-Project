package id.my.rascal.invoice.internal.model.response;

import java.time.LocalDateTime;
import java.util.List;

public record RefundResponse(
    Long id,
    Long invoiceId,
    Long paymentId,
    Integer scopeAmount,
    Integer cashAmount,
    List<Long> orderItemIds,
    LocalDateTime createdAt,
    Long createdBy
) {}
