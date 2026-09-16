package id.my.rascal.payment.api;

import java.time.LocalDateTime;

public interface PaymentReportApi {

    long sumAppliedSettledBetween(LocalDateTime from, LocalDateTime to);

}
