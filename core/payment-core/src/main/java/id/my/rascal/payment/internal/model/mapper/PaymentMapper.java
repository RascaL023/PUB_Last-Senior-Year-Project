package id.my.rascal.payment.internal.model.mapper;

import id.my.rascal.payment.api.PaymentProcessorStatus;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;

public class PaymentMapper {

    PaymentMapper() {}

    public static PaymentStatus toPaymentStatus(PaymentProcessorStatus status) {
        return switch (status) {
            case PENDING -> PaymentStatus.PENDING;
            case PAID -> PaymentStatus.PAID;
            case FAILED -> PaymentStatus.FAILED;
            case EXPIRED -> PaymentStatus.EXPIRED;
            case REFUNDED -> PaymentStatus.REFUNDED;
        };
    }
    
}
