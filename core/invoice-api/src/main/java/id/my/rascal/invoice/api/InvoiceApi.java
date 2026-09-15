package id.my.rascal.invoice.api;

import java.util.Collection;
import java.util.List;

public interface InvoiceApi {

    InvoiceApiResponse getInvoice(Long id);
    List<InvoiceApiResponse> getInvoices(Collection<Long> ids);
    InvoiceApiResponse getDiningInvoice(Long diningId);
    InvoiceApiResponse applyPayment(Long invoiceId, Integer amount);
    InvoiceApiResponse refundItems(Long invoiceId, List<Long> orderItemIds, Long paymentId);
    boolean hasAppliedPayment(Long orderId);

}
