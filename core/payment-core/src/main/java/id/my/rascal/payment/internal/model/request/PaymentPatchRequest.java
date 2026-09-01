package id.my.rascal.payment.internal.model.request;

import java.util.Optional;

import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;

public record PaymentPatchRequest(
    Optional<PaymentTargetType> targetType,
    Optional<Long> targetId,
    Optional<PaymentProvider> paymentProvider,
    Optional<String> paymentDetail
) {
    public PaymentPatchRequest(
        PaymentTargetType targetType,
        Long targetId,
        PaymentProvider paymentProvider,
        String paymentDetail
    ) {
        this(
            Optional.ofNullable(targetType),
            Optional.ofNullable(targetId),
            Optional.ofNullable(paymentProvider),
            Optional.ofNullable(paymentDetail)
        );
    }

    public boolean isEmptyPatch() {
        return targetType.isEmpty()
            && targetId.isEmpty()
            && paymentDetail.isEmpty()
            && paymentProvider.isEmpty();
    }
}
