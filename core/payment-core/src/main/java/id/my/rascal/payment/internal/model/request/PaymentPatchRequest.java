package id.my.rascal.payment.internal.model.request;

import java.util.Optional;

import id.my.rascal.payment.internal.model.enums.PaymentTargetType;

public record PaymentPatchRequest(
    Optional<PaymentTargetType> targetType,
    Optional<Long> targetId,
    Optional<Long> paymentMethodId,
    Optional<String> paymentChannel,
    Optional<String> paymentDetail,
    Optional<String> externalId,
    Optional<String> invoiceUrl
) {
    public PaymentPatchRequest(
        PaymentTargetType targetType,
        Long targetId,
        Long paymentMethodId,
        String paymentChannel,
        String paymentDetail,
        String externalId,
        String invoiceUrl
    ) {
        this(
            Optional.ofNullable(targetType),
            Optional.ofNullable(targetId),
            Optional.ofNullable(paymentMethodId),
            Optional.ofNullable(paymentChannel),
            Optional.ofNullable(paymentDetail),
            Optional.ofNullable(externalId),
            Optional.ofNullable(invoiceUrl)
        );
    }

    public boolean isEmptyPatch() {
        return targetType.isEmpty()
            && targetId.isEmpty()
            && paymentMethodId.isEmpty()
            && paymentChannel.isEmpty()
            && paymentDetail.isEmpty()
            && externalId.isEmpty()
            && invoiceUrl.isEmpty();
    }
}
