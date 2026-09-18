package id.my.rascal.invoice.api;

import java.util.Collection;
import java.util.List;

public interface InvoiceApi {

    InvoiceApiResponse getInvoice(Long id);
    List<InvoiceApiResponse> getInvoices(Collection<Long> ids);
    InvoiceApiResponse getDiningInvoice(Long diningId);
    boolean hasAppliedPayment(Long orderId);
    String findStatusByOrderId(Long orderId);

}
