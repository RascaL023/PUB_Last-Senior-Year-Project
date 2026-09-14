package id.my.rascal.payment.internal.model.request;

import java.util.List;

public record PaymentRefundRequest(
    List<Long> orderItemIds
) {}
