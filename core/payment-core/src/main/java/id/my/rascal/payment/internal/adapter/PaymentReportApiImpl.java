package id.my.rascal.payment.internal.adapter;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.payment.api.PaymentReportApi;
import id.my.rascal.payment.internal.repository.PaymentReportRepository;

@Component
public class PaymentReportApiImpl implements PaymentReportApi {

    private final PaymentReportRepository paymentReportRepository;

    public PaymentReportApiImpl(PaymentReportRepository paymentReportRepository) {
        this.paymentReportRepository = paymentReportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public long sumPaidAmount(LocalDateTime from, LocalDateTime to) {
        return paymentReportRepository.sumAmountByStatusInPeriod(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPaidOrders(LocalDateTime from, LocalDateTime to) {
        return paymentReportRepository.countDistinctTargetsByStatusAndTargetTypeInPeriod(from, to);
    }

}