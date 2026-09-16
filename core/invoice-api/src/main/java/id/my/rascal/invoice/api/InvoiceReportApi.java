package id.my.rascal.invoice.api;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InvoiceReportApi {

    BillingMetrics billingMetrics(LocalDateTime from, LocalDateTime to);
    List<MenuSalesRow> topMenuSales(LocalDateTime from, LocalDateTime to, int limit);
    Map<Long, String> billingStatusByOrderIds(Collection<Long> orderIds);

    record BillingMetrics(
        long settledInvoices,
        long settledAmount,
        long outstandingInvoices,
        long outstandingAmount
    ) {
        public long averageSettledInvoice() {
            return settledInvoices == 0 ? 0 : settledAmount / settledInvoices;
        }
    }

    record MenuSalesRow(
        Long menuId,
        String description,
        long qty,
        long revenue
    ) {}

}
