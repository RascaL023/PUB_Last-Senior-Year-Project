package id.my.rascal.payment.api;

import java.time.LocalDateTime;

public interface PaymentReportApi {

    long sumPaidAmount(LocalDateTime from, LocalDateTime to);
    long countPaidOrders(LocalDateTime from, LocalDateTime to);

}
