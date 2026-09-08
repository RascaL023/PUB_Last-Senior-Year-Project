package id.my.rascal.invoice.internal.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.invoice.internal.model.mapper.InvoiceMapper;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;
import id.my.rascal.invoice.internal.service.InvoiceService;

@Service
public class InvoiceApiImpl implements InvoiceApi {

    private final InvoiceQueryService invoiceQueryService;
    private final InvoiceService invoiceService;

    public InvoiceApiImpl(
        InvoiceQueryService invoiceQueryService,
        InvoiceService invoiceService
    ) {
        this.invoiceQueryService = invoiceQueryService;
        this.invoiceService = invoiceService;
    }

    @Override
    public InvoiceApiResponse getInvoice(Long id) {
        return InvoiceMapper.toApiResponse(invoiceQueryService.findActiveInvoiceById(id));
    }

    @Override
    public List<InvoiceApiResponse> getInvoices(Collection<Long> ids) {
        return invoiceQueryService.findActiveInvoicesByIds(ids);
    }

    @Override
    public InvoiceApiResponse applyPayment(Long invoiceId, Integer amount) {
        return InvoiceMapper.toApiResponse(invoiceService.applyPayment(invoiceId, amount));
    }

}
